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

import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.filters.word.result.FilterResult;
import com.sparkword.model.MuteInfo;
import com.sparkword.model.MuteInfo.MuteScope;
import com.sparkword.spammers.SpamManager.SpamResult;
import com.sparkword.util.TimeUtil;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final SparkWord plugin;

    public ChatListener(SparkWord plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        ConfigManager cfg = plugin.getEnvironment().getConfigManager();
        if (!cfg.isFilterChat()) return;

        Player player = event.getPlayer();

        int pid = plugin.getEnvironment().getPlayerDataManager().getPlayerId(player.getUniqueId(), player.getName());
        MuteInfo muteInfo = plugin.getEnvironment().getPlayerDataManager().getMuteInfo(pid);

        if (muteInfo.blocks(MuteScope.CHAT)) {
            event.setCancelled(true);
            String timeLeft = muteInfo.isPermanent() ? "Permanent" : TimeUtil.formatDuration((muteInfo.expiry() - System.currentTimeMillis()) / 1000);
            plugin.getEnvironment().getMessageManager().sendMessage(player, "player-muted",
                Map.of("staff", muteInfo.staff(), "time", timeLeft, "reason", muteInfo.reason()));
            return;
        }

        if (player.hasPermission("sparkword.bypass.chat")) return;

        Component originalComponent = event.message();
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(originalComponent);

        FilterResult result = plugin.getEnvironment().getFilterManager().processText(plainMessage, false, player);

        if (result.blocked()) {
            logFilteredMessage(player, plainMessage);
            blockAndNotify(player, event, result.reason(), plainMessage, result.detectedWord());
            return;
        }

        Component processedComponent = originalComponent;
        String messageToCheck = plainMessage;

        if (!result.detectedWords().isEmpty()) {
            if (cfg.isReplacementEnabled()) {
                String replacement = cfg.getGlobalReplacement();
                Component replacementComp = MiniMessage.miniMessage().deserialize(replacement);

                for (String word : result.detectedWords()) {
                    Pattern pattern = plugin.getEnvironment().getFilterManager().getCachedPattern(word);

                    TextReplacementConfig textConfig = TextReplacementConfig.builder()
                        .match(pattern)
                        .replacement(replacementComp)
                        .build();
                    processedComponent = processedComponent.replaceText(textConfig);
                }

                String processedPlain = PlainTextComponentSerializer.plainText().serialize(processedComponent);

                if (processedPlain.equals(plainMessage)) {
                    logFilteredMessage(player, plainMessage + " [EVASION DETECTED]");
                    blockAndNotify(player, event, "Evasion/Hidden Characters", plainMessage, result.detectedWords().iterator().next());
                    return;
                } else {

                    logFilteredMessage(player, plainMessage + " -> " + processedPlain);
                    event.message(processedComponent);
                    messageToCheck = processedPlain;

                    if (cfg.isNotifyIconEnabled()) {
                        ChatRenderer previousRenderer = event.renderer();

                        event.renderer((source, sourceDisplayName, message, viewer) -> {
                            Component rendered = previousRenderer.render(source, sourceDisplayName, message, viewer);

                            if (viewer instanceof CommandSender sender && sender.hasPermission("sparkword.notify.icon")) {
                                return rendered.append(plugin.getEnvironment().getMessageManager().getSpyIconComponent(plainMessage, result.detectedWords(), true));
                            }

                            return rendered;
                        });
                    }
                }
            } else {
                logFilteredMessage(player, plainMessage);
                blockAndNotify(player, event, "Filter (Total Block)", plainMessage, result.detectedWords().iterator().next());
                return;
            }
        }

        SpamResult spamResult = plugin.getEnvironment().getSpamManager().checkSpam(
            player, messageToCheck, "Chat", false, null, -1, true
        );

        if (spamResult.blocked()) {
            event.setCancelled(true);
            logFilteredMessage(player, plainMessage + " (SPAM)");
            plugin.getEnvironment().getMessageManager().sendMessage(player, spamResult.reasonKey());
            return;
        }

        if (spamResult.modified()) {
            event.message(MiniMessage.miniMessage().deserialize(spamResult.modifiedMessage()));
        }
    }

    private void logFilteredMessage(Player player, String originalMessage) {
        if (plugin.isDebugFilter()) {
            plugin.getLogger().info("[Debug] [" + player.getName() + "] " + originalMessage);
        }
    }

    private void blockAndNotify(Player player, AsyncChatEvent event, String reasonKey, String content, String detected) {
        event.setCancelled(true);
        String displayReason = reasonKey != null ? reasonKey : "Filter";
        plugin.getEnvironment().getMessageManager().sendMessage(player, "notify-blocked", Map.of("reason", displayReason));
        plugin.getEnvironment().getNotifyManager().notifyStaff(player, "Chat (" + displayReason + ")", content, detected);
    }
}
