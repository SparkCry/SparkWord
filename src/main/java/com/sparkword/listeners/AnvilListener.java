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
package com.sparkword.listeners;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sparkword.SparkWord;
import com.sparkword.filters.word.result.FilterResult;
import com.sparkword.model.MuteInfo;
import com.sparkword.model.MuteInfo.MuteScope;
import com.sparkword.spammers.SpamManager.SpamResult;
import com.sparkword.util.StringUtil;
import com.sparkword.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class AnvilListener implements Listener {
    private final SparkWord plugin;

    private final Cache<UUID, Long> notifyCooldown;
    private final Cache<UUID, Long> lastCheckTime;

    public AnvilListener(SparkWord plugin) {
        this.plugin = plugin;

        this.notifyCooldown = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .build();

        this.lastCheckTime = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(1))
            .build();
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (!plugin.getEnvironment().getConfigManager().isFilterAnvils()) return;
        if (!(event.getView().getPlayer() instanceof Player p)) return;

        long now = System.currentTimeMillis();
        Long lastCheck = lastCheckTime.getIfPresent(p.getUniqueId());
        if (lastCheck != null && now - lastCheck < 200) return;
        lastCheckTime.put(p.getUniqueId(), now);

        var env = plugin.getEnvironment();
        int pid = env.getPlayerDataManager().getPlayerId(p.getUniqueId(), p.getName());
        MuteInfo muteInfo = env.getPlayerDataManager().getMuteInfo(pid);

        if (muteInfo.blocks(MuteScope.GLOBAL)) {
            event.setResult(null);
            if (checkNotifyCooldown(p)) {
                String timeLeft = (muteInfo.expiry() == 0) ? "Permanent" : TimeUtil.formatDuration((muteInfo.expiry() - System.currentTimeMillis()) / 1000);
                env.getMessageManager().sendMessage(p, "player-muted", Map.of("staff", muteInfo.staff(), "time", timeLeft, "reason", "PERMUTE (Interaction blocked)"));
            }
            return;
        }

        if (p.hasPermission("sparkword.bypass.anvils")) return;

        String text = null;
        if (event.getView() instanceof AnvilView view) {
            text = view.getRenameText();
        } else { return; }

        if (text == null || text.isBlank()) return;

        SpamResult spam = env.getSpamManager().checkSpam(p, text, "Anvil", true, null, -1, false);
        if (spam.blocked()) {
            event.setResult(null);
            if (checkNotifyCooldown(p)) env.getMessageManager().sendMessage(p, spam.reasonKey());
            return;
        }

        FilterResult result = env.getFilterManager().processText(text, true, p);

        if (result.blocked()) {
            event.setResult(null);
            if (checkNotifyCooldown(p)) {
                String reason = result.reason() != null ? result.reason() : "Filter";

                Component msg = env.getMessageManager().getComponent("prevention.blocked-action.anvil", Collections.emptyMap(), false);
                p.sendMessage(msg.append(Component.text(" (" + reason + ").", NamedTextColor.RED)));

                env.getNotifyManager().notifyEdit(p, "Anvil", reason, result.detectedWord(), text);
            }
        } else if (!result.detectedWords().isEmpty()) {

            String detected = result.detectedWords().iterator().next();
            if (!StringUtil.containsIgnoreCase(text, detected)) {
                event.setResult(null);
                if (checkNotifyCooldown(p)) {
                    Component forbidden = env.getMessageManager().getComponent("prevention.blocked-action.anvil", Collections.emptyMap(), false);
                    Component evasion = env.getMessageManager().getComponent("spam.evasion", Collections.emptyMap(), false);

                    p.sendMessage(forbidden
                        .append(Component.text(" (", NamedTextColor.RED))
                        .append(evasion)
                        .append(Component.text(").", NamedTextColor.RED)));

                    env.getNotifyManager().notifyEdit(p, "Anvil", "Evasion", detected, text);
                }
                return;
            }

            ItemStack originalResult = event.getResult();
            if (originalResult != null && originalResult.getType() != org.bukkit.Material.AIR) {
                ItemStack censoredItem = originalResult.clone();
                ItemMeta meta = censoredItem.getItemMeta();
                if (meta != null && result.processedMessage() != null) {

                    String replacementString = result.processedMessage();
                    Component replacementComp = MiniMessage.miniMessage().deserialize(replacementString);

                    meta.displayName(replacementComp);
                    censoredItem.setItemMeta(meta);
                    event.setResult(censoredItem);
                }
            }
        }
    }

    private boolean checkNotifyCooldown(Player p) {
        if (notifyCooldown.getIfPresent(p.getUniqueId()) == null) {
            notifyCooldown.put(p.getUniqueId(), System.currentTimeMillis());
            return true;
        }
        return false;
    }
}
