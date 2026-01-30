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
package com.sparkword.commands.impl.root;

import com.sparkword.Environment;
import com.sparkword.commands.SubCommand;
import org.bukkit.command.CommandSender;

public class DebugCommand implements SubCommand {
    private final Environment env;

    public DebugCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.debug")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("filter")) {
            boolean newState = !env.getPlugin().isDebugFilter();
            env.getPlugin().setDebugFilter(newState);

            String key = newState ? "debug.filter-enabled" : "debug.filter-disabled";
            env.getMessageManager().sendMessage(sender, key);
            return true;
        }

        boolean newState = !env.getPlugin().isDebugMode();
        env.getPlugin().setDebugMode(newState);

        String key = newState ? "debug.general-enabled" : "debug.general-disabled";
        env.getMessageManager().sendMessage(sender, key);
        return true;
    }
}
