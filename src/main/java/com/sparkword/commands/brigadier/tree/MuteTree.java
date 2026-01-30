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
package com.sparkword.commands.brigadier.tree;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sparkword.Environment;
import com.sparkword.commands.CommandManager;
import com.sparkword.commands.brigadier.BrigadierNode;
import com.sparkword.commands.brigadier.BrigadierUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MuteTree implements BrigadierNode {

    @Override
    public void register(Commands commands, CommandManager manager, Environment env, Map<String, LiteralCommandNode<CommandSourceStack>> registry) {
        List.of("sw-permute", "sw-mute", "sw-warn", "sw-checkmute", "sw-unmute", "sw-tempmute")
            .forEach(cmdName -> registerSingle(commands, manager, cmdName, registry));
    }

    private void registerSingle(Commands commands, CommandManager manager, String cmdName, Map<String, LiteralCommandNode<CommandSourceStack>> registry) {
        var builder = Commands.literal(cmdName)
            .requires(src -> src.getSender().hasPermission("sparkword." + cmdName.replace("sw-", "")))
            .executes(ctx -> run(manager, ctx, cmdName));

        var playerArg = Commands.argument("player", StringArgumentType.word())
            .suggests(BrigadierUtils::suggestOnlinePlayers)
            .executes(ctx -> run(manager, ctx, cmdName, StringArgumentType.getString(ctx, "player")));

        if (cmdName.equals("sw-tempmute")) {
            playerArg.then(Commands.argument("time", StringArgumentType.word())
                .suggests(BrigadierUtils::suggestDurations)
                .executes(ctx -> run(manager, ctx, cmdName, StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "time")))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> run(manager, ctx, cmdName, StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "time"), StringArgumentType.getString(ctx, "reason")))
                )
            );
        } else if (!cmdName.equals("sw-checkmute") && !cmdName.equals("sw-unmute")) {
            playerArg.then(Commands.argument("reason", StringArgumentType.greedyString())
                .suggests(BrigadierUtils::suggestPresets)
                .executes(ctx -> run(manager, ctx, cmdName, StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "reason")))
            );
        }

        builder.then(playerArg);

        // Fix: Build node first, then register, then store in registry
        LiteralCommandNode<CommandSourceStack> node = builder.build();
        commands.register(node, "SparkWord " + cmdName, Collections.emptyList());
        registry.put(cmdName, node);
    }
}
