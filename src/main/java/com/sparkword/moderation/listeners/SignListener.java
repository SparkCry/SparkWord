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
package com.sparkword.moderation.listeners;

import com.sparkword.SparkWord;
import com.sparkword.core.storage.model.MuteInfo;
import com.sparkword.core.storage.model.MuteInfo.MuteScope;
import com.sparkword.moderation.antispam.SpamManager.SpamResult;
import com.sparkword.moderation.filters.word.result.FilterResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Map;
import java.util.regex.Pattern;

public class SignListener implements Listener {

    private final SparkWord plugin;

    public SignListener(SparkWord plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!plugin.getEnvironment().getConfigManager().isFilterSigns()) return;

        Player player = event.getPlayer();

        int pid = plugin.getEnvironment().getPlayerDataManager().getPlayerId(player.getUniqueId(), player.getName());
        MuteInfo muteInfo = plugin.getEnvironment().getPlayerDataManager().getMuteInfo(pid);

        if (muteInfo.blocks(MuteScope.GLOBAL)) {
            event.setCancelled(true);
            plugin.getEnvironment().getMessageManager().sendMessage(player, "moderation.player-permuted");
            return;
        }

        if (player.hasPermission("sparkword.bypass.sign")) return;

        boolean hasContent = false;
        for (Component line : event.lines()) {
            if (line != null && !PlainTextComponentSerializer.plainText().serialize(line).isEmpty()) {
                hasContent = true;
                break;
            }
        }
        if (!hasContent) return;

        for (int i = 0; i < 4; i++) {
            Component lineComponent = event.line(i);
            if (lineComponent == null) continue;

            String plainLine = PlainTextComponentSerializer.plainText().serialize(lineComponent);
            if (plainLine.length() < 1) continue;

            FilterResult filterResult = plugin.getEnvironment().getFilterManager().processText(plainLine, true, player);

            if (filterResult.blocked()) {
                event.setCancelled(true);
                plugin.getEnvironment().getMessageManager().sendMessage(player, "notification.blocked", Map.of("reason", filterResult.reason()));

                plugin.getEnvironment().getNotifyManager().notifySignBlocked(player, filterResult.reason(), filterResult.detectedWord(), getPlainLines(event));
                return;
            }

            if (!filterResult.detectedWords().isEmpty()) {
                String replacement = plugin.getEnvironment().getConfigManager().getGlobalReplacement();

                Component replacementComp = MiniMessage.miniMessage().deserialize(replacement);

                Component processed = lineComponent;
                for (String word : filterResult.detectedWords()) {
                    Pattern pattern = plugin.getEnvironment().getFilterManager().getCachedPattern(word);
                    processed = processed.replaceText(TextReplacementConfig.builder()
                        .match(pattern)
                        .replacement(replacementComp)
                        .build());
                }

                if (processed.equals(lineComponent)) {
                    event.setCancelled(true);
                    plugin.getEnvironment().getMessageManager().sendMessage(player, "notification.blocked", Map.of("reason", "Evasion"));
                    plugin.getEnvironment().getNotifyManager().notifySignBlocked(player, "Evasion", filterResult.detectedWord(), getPlainLines(event));
                    return;
                }

                event.line(i, processed);
                plainLine = PlainTextComponentSerializer.plainText().serialize(processed);
            }

            SpamResult spamResult = plugin.getEnvironment().getSpamManager().checkSpam(
                player, plainLine, "Sign", true, event.getBlock().getLocation(), i, true
            );

            if (spamResult.blocked()) {
                event.setCancelled(true);
                plugin.getEnvironment().getMessageManager().sendMessage(player, spamResult.reasonKey());

                return;
            }

            if (spamResult.modified()) {
                event.line(i, MiniMessage.miniMessage().deserialize("<red>" + spamResult.modifiedMessage()));
            }
        }
    }

    private String[] getPlainLines(SignChangeEvent event) {
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = PlainTextComponentSerializer.plainText().serialize(event.line(i));
        }
        return lines;
    }
}
