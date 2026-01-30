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
package com.sparkword.commands.brigadier;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sparkword.Environment;
import com.sparkword.commands.CommandManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Map;

public interface BrigadierNode {

    // Updated to accept a registry map for alias redirection
    void register(Commands commands, CommandManager manager, Environment env, Map<String, LiteralCommandNode<CommandSourceStack>> nodeRegistry);

    default int run(CommandManager manager, CommandContext<CommandSourceStack> ctx, String label, String... args) {
        manager.dispatchFromBrigadier(ctx.getSource().getSender(), label, args);
        return 1;
    }
}
