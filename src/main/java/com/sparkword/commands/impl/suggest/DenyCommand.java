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
package com.sparkword.commands.impl.suggest;

import com.sparkword.commands.SubCommand;
import com.sparkword.core.Environment;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class DenyCommand implements SubCommand {
    private final Environment env;
    public DenyCommand(Environment env) { this.env = env; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.deny")) {
             env.getMessageManager().sendMessage(sender, "no-permission");
             return true;
        }
        if (args.length < 1) {
            env.getMessageManager().sendMessage(sender, "usage-deny");
            return true;
        }

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (Exception e) {
            env.getMessageManager().sendMessage(sender, "error-invalid-id");
            return true;
        }

        final String staffName = sender.getName();

        env.getStorage().denySuggestionAsync(id, staffName, info -> {
            Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                if (info != null) {
                    env.getMessageManager().sendMessage(sender, "action-suggest-rejected");

                    Player suggester = Bukkit.getPlayer(info.playerUUID());
                    if (suggester != null && suggester.isOnline()) {
                        env.getMessageManager().sendMessage(suggester, "suggest-deny", Map.of("word", info.word(), "staff", staffName));
                    }
                } else {
                    env.getMessageManager().sendMessage(sender, "error-id-not-found");
                }
            });
        });

        return true;
    }
}
