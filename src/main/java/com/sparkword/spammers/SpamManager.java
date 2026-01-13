/*
 * This file is part of SparkWord - https://github.com/SparkCry/SparkWord
 * Copyright (C) 2026 SparkCry and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.sparkword.spammers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sparkword.SparkWord;
import com.sparkword.core.SQLiteStorage;
import com.sparkword.model.MuteInfo.MuteScope;
import com.sparkword.spammers.checks.*;
import com.sparkword.spammers.security.InputSanitizer;
import com.sparkword.util.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class SpamManager {

    private final SparkWord plugin;
    private final List<SpamCheck> activeChecks = new ArrayList<>();
    private final InputSanitizer inputSanitizer;

    private final Cache<UUID, Long> autoMuteCooldown;

    private static final int MAX_SAFE_LENGTH = 1000;
    private static final Pattern INVISIBLE_CHARS = Pattern.compile("[\\p{Cc}\\p{Cs}\\p{Co}\\p{Cf}&&[^\\n\\u200D\\u200C]]");

    public enum PunishmentType { NONE, MUTE, PERMUTE }

    public record SpamResult(boolean blocked, String message, String reasonKey, boolean modified, PunishmentType punishment) {
        public String modifiedMessage() { return message; }
        public static SpamResult PASSED = new SpamResult(false, null, null, false, PunishmentType.NONE);

        public static SpamResult BLOCKED_WITH_REASON(String reasonKey, boolean modified) {
            return new SpamResult(true, null, reasonKey, modified, PunishmentType.NONE);
        }

        public static SpamResult BLOCKED_WITH_REASON(String reasonKey, boolean modified, PunishmentType punishment) {
            return new SpamResult(true, null, reasonKey, modified, punishment);
        }

        public static SpamResult MODIFIED(String modifiedMessage) {
            return new SpamResult(false, modifiedMessage, null, true, PunishmentType.NONE);
        }
    }

    public SpamManager(SparkWord plugin) {
        this.plugin = plugin;
        this.inputSanitizer = new InputSanitizer(plugin);

        this.autoMuteCooldown = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .build();

        initChecks();
        reload();
    }

    private void initChecks() {
        activeChecks.clear();

        activeChecks.add(new AntiFloodCheck(plugin));

        activeChecks.add(new IPCheck(plugin));
        activeChecks.add(new IPSplitCheck(plugin));
        activeChecks.add(new DomainCheck(plugin));
        activeChecks.add(new DigitsLimitCheck(plugin));
        activeChecks.add(new CharSpamCheck(plugin));
        activeChecks.add(new AntiRepeatCheck(plugin));
        activeChecks.add(new CapsCheck(plugin));
    }

    public void reload() {
        for(SpamCheck check : activeChecks) {
            if (check instanceof AntiFloodCheck f) f.reload();
            if (check instanceof CharSpamCheck c) c.reload();
        }
    }

    public void cleanupPlayer(UUID uuid) {
        autoMuteCooldown.invalidate(uuid);
        for(SpamCheck check : activeChecks) {
            if (check instanceof IPCheck ip) ip.cleanupPlayer(uuid);
            if (check instanceof IPSplitCheck ips) ips.clearHistory(uuid);
        }
    }

    public void triggerAutoMute(Player p, String configPath, String defaultTime, String reason, PunishmentType type) {
        if (type == PunishmentType.NONE) return;

        if (autoMuteCooldown.getIfPresent(p.getUniqueId()) != null) return;
        autoMuteCooldown.put(p.getUniqueId(), System.currentTimeMillis());

        final String actionType = (type == PunishmentType.PERMUTE) ? "PERMUTE" : "MUTE";
        final MuteScope scope = (type == PunishmentType.PERMUTE) ? MuteScope.GLOBAL : MuteScope.CHAT;

        String muteTimeStr = plugin.getConfig().getString(configPath, defaultTime);
        long muteSeconds = TimeUtil.parseDuration(muteTimeStr);

        if (muteSeconds > 0 || (type == PunishmentType.PERMUTE && muteSeconds == 0)) {
            int cachedId = plugin.getEnvironment().getPlayerDataManager().getPlayerId(p.getUniqueId(), p.getName());

            if (cachedId != -1) {
                applyMute(cachedId, p, reason, muteSeconds, actionType, scope);
            } else {
                plugin.getEnvironment().getStorage().getPlayerIdAsync(p.getUniqueId(), p.getName())
                    .thenAccept(pid -> {
                        if (pid != -1) {
                            applyMute(pid, p, reason, muteSeconds, actionType, scope);
                        }
                    });
            }
        }
    }

    private void applyMute(int pid, Player p, String reason, long muteSeconds, String actionType, MuteScope scope) {
        plugin.getEnvironment().getStorage().mute(pid, reason, SQLiteStorage.SYSTEM_ACTOR, muteSeconds, actionType, scope, success -> {
            if (success && p.isOnline()) {
                String timeFormatted = (muteSeconds == 0) ? "Permanent" : TimeUtil.formatDuration(muteSeconds);
                plugin.getEnvironment().getMessageManager().sendMessage(
                    p,
                    "player-muted",
                    Map.of(
                        "staff", SQLiteStorage.SYSTEM_ACTOR,
                        "reason", reason,
                        "time", timeFormatted
                    )
                );
            }
        });
    }

    public SpamResult checkSpam(Player player, String message, String source, boolean isWritable, Location signLocation, int lineIndex, boolean checkTraffic) {
        var cfg = plugin.getEnvironment().getConfigManager();
        if (!cfg.isAntiSpamEnabled() || player.hasPermission("sparkword.bypass.spam")) {
            return SpamResult.PASSED;
        }

        if (message.length() > MAX_SAFE_LENGTH) {
            return SpamResult.BLOCKED_WITH_REASON("spam.chars", false);
        }

        String sanitizedMsg = INVISIBLE_CHARS.matcher(message).replaceAll("");
        sanitizedMsg = Normalizer.normalize(sanitizedMsg, Normalizer.Form.NFKC);
        String cleanMsg = stripTags(sanitizedMsg);

        SpamContext context = new SpamContext(message, cleanMsg, source, isWritable, signLocation, lineIndex, checkTraffic);

        boolean modified = false;
        String finalMessage = sanitizedMsg;

        if (cfg.isAntiInjectionEnabled() && !player.hasPermission("sparkword.bypass.injection")) {
            String safe = inputSanitizer.sanitize(finalMessage, player);
            if (!safe.equals(finalMessage)) {
                plugin.getEnvironment().getNotifyManager().notifyInjection(player, source, message);
                if (cfg.isReplacementEnabled()) {
                    finalMessage = safe;
                    modified = true;
                    context = new SpamContext(finalMessage, stripTags(finalMessage), source, isWritable, signLocation, lineIndex, checkTraffic);
                } else {
                    return SpamResult.BLOCKED_WITH_REASON("spam.injection", false);
                }
            }
        }

        for (SpamCheck check : activeChecks) {
            SpamResult result = check.check(player, context);
            if (result.blocked()) return result;
            if (result.modified()) {
                finalMessage = result.message();
                modified = true;
                context = new SpamContext(finalMessage, stripTags(finalMessage), source, isWritable, signLocation, lineIndex, checkTraffic);
            }
        }

        if (modified) return SpamResult.MODIFIED(finalMessage);

        return SpamResult.PASSED;
    }

    private String stripTags(String text) {
        if (text == null) return "";
        return MiniMessage.miniMessage().stripTags(text);
    }
}
