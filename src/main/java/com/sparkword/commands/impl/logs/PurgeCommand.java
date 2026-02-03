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
package com.sparkword.commands.impl.logs;

import com.sparkword.Environment;
import com.sparkword.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class PurgeCommand implements SubCommand {
    private final Environment env;

    public PurgeCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            env.getMessageManager().sendMessage(sender, "help.usage-purge");
            return true;
        }
        String type = args[0].toLowerCase();
        if (type.equals("l")) type = "b";

        String perm = "sparkword.purge.logs";
        if (type.equals("all")) perm = "sparkword.admin";
        else if (type.equals("sg")) perm = "sparkword.purge.suggest";
        else if (type.equals("w")) perm = "sparkword.purge.warning";
        else if (type.equals("m")) perm = "sparkword.purge.mute";
        else if (type.equals("a")) perm = "sparkword.purge.audit";
        else if (type.equals("hp")) perm = "sparkword.purge.history";

        if (!sender.hasPermission(perm)) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        int days;
        try {
            days = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            env.getMessageManager().sendMessage(sender, "error-invalid-days");
            return true;
        }
        if (days < 0) {
            env.getMessageManager().sendMessage(sender, "error-negative");
            return true;
        }

        env.getMessageManager().sendMessage(sender, "moderation.action-purge-start");

        final String fType = type;
        Bukkit.getScheduler().runTaskAsynchronously(env.getPlugin(), () -> {

            int deleted = env.getStorage().purgeData(fType, days);

            env.getMessageManager().sendMessage(sender, "moderation.action-purge-complete", Map.of("count", String.valueOf(deleted)));

            env.getStorage().logAudit(sender.getName(), "PURGE", "Type: " + fType + " | Days: " + days);
        });
        return true;
    }
}
