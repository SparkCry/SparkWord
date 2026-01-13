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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

public class WarnCommand implements SubCommand {
    private final Environment env;
    public WarnCommand(Environment env) { this.env = env; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.warn")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            env.getMessageManager().sendMessage(sender, "usage-warn");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { env.getMessageManager().sendMessage(sender, "player-not-found"); return true; }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        int pid = env.getPlayerDataManager().getPlayerId(target.getUniqueId(), target.getName());

        if (pid != -1) {
            env.getStorage().addWarning(pid, reason, sender.getName());
        } else {
            env.getStorage().getPlayerIdAsync(target.getUniqueId(), target.getName())
                .thenAccept(id -> env.getStorage().addWarning(id, reason, sender.getName()));
        }

        env.getMessageManager().sendMessage(target, "warn-receive", Map.of("staff", sender.getName(), "reason", reason));
        env.getMessageManager().sendMessage(sender, "warn-sent", Map.of("player", target.getName()));
        return true;
    }
}
