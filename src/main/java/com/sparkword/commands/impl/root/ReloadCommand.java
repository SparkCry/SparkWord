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

import com.sparkword.commands.SubCommand;
import com.sparkword.core.Environment;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements SubCommand {
    private final Environment env;
    public ReloadCommand(Environment env) { this.env = env; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.reload")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }
        env.getPlugin().reload();
        env.getMessageManager().sendMessage(sender, "reload");
        return true;
    }
}
