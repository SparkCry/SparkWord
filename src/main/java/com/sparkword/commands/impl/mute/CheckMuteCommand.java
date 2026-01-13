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
package com.sparkword.commands.impl.mute;

import com.sparkword.commands.SubCommand;
import com.sparkword.core.Environment;
import com.sparkword.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class CheckMuteCommand implements SubCommand {
    private final Environment env;
    public CheckMuteCommand(Environment env) { this.env = env; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.checkmute")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            env.getMessageManager().sendMessage(sender, "usage-checkmute");
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        int cachedId = env.getPlayerDataManager().getPlayerId(target.getUniqueId(), target.getName());

        if (cachedId != -1) {
            check(sender, args[0], cachedId);
        } else {
            env.getStorage().getPlayerIdAsync(target.getUniqueId(), target.getName() != null ? target.getName() : args[0])
                .thenAccept(id -> check(sender, args[0], id));
        }
        return true;
    }

    private void check(CommandSender sender, String name, int pid) {
        env.getStorage().getMuteTimeAsync(pid).thenAccept(exp -> {
            boolean m = exp != -1 && (exp == 0 || exp > System.currentTimeMillis());
            Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                if (m) {
                    String time = exp==0 ? "Perm" : TimeUtil.formatDuration((exp-System.currentTimeMillis())/1000);
                    env.getMessageManager().sendMessage(sender, "check-mute-true", Map.of("player", name, "time", time));
                } else {
                    env.getMessageManager().sendMessage(sender, "check-mute-false", Map.of("player", name));
                }
            });
        });
    }
}
