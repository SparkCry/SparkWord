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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

public class CommandListener implements Listener {

    private static final int MAX_COMMAND_LENGTH = 256;
    private static final Set<String> BLOCKED_CMDS = Set.of(
        "msg", "tell", "w", "whisper", "r", "reply", "mail", "me", "say", "nick", "nickname", "tags", "rename"
    );
    private final SparkWord plugin;

    public CommandListener(SparkWord plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;

        String msg = event.getMessage();

        if (msg.length() > MAX_COMMAND_LENGTH && !event.getPlayer().hasPermission("sparkword.bypass.spam")) {
            event.setCancelled(true);

            plugin.getEnvironment().getMessageManager().sendMessage(event.getPlayer(), "error-command-length");
            return;
        }

        int pid = plugin.getEnvironment().getPlayerDataManager().getPlayerId(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        MuteInfo muteInfo = plugin.getEnvironment().getPlayerDataManager().getMuteInfo(pid);

        if (muteInfo.blocks(MuteScope.GLOBAL)) {
            String lowerMsg = msg.toLowerCase();
            String[] parts = lowerMsg.split(" ");

            if (parts.length > 0) {
                String cmd = parts[0].substring(1);

                if (BLOCKED_CMDS.contains(cmd)) {
                    event.setCancelled(true);
                    plugin.getEnvironment().getMessageManager().sendMessage(event.getPlayer(), "moderation.player-permuted");
                    return;
                }
            }
        }

        if (event.getPlayer().hasPermission("sparkword.bypass.writecommands")) {
            return;
        }

        String[] parts = msg.split(" ", 2);

        if (parts.length < 2) return;

        String command = parts[0].substring(1);
        String args = parts[1];

        if (args.isEmpty()) return;

        SpamResult spam = plugin.getEnvironment().getSpamManager().checkSpam(
            event.getPlayer(),
            args,
            "Command: " + command,
            true,
            null,
            -1,
            false
        );

        if (spam.blocked()) {
            event.setCancelled(true);
            plugin.getEnvironment().getMessageManager().sendMessage(event.getPlayer(), spam.reasonKey());
            plugin.getEnvironment().getNotifyManager().notifyEdit(
                event.getPlayer(),
                "Command",
                "Spam/Logic",
                "Restriction Triggered",
                msg
            );
        }
    }
}
