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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sparkword.Environment;
import com.sparkword.commands.CommandManager;
import com.sparkword.commands.brigadier.BrigadierNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Collections;
import java.util.Map;

public class SystemTree implements BrigadierNode {

    public static void attachSubCommands(LiteralArgumentBuilder<CommandSourceStack> swNode, CommandManager manager) {
        swNode.then(Commands.literal("info")
                .requires(s -> s.getSender().hasPermission("sparkword.info"))
                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "info");
                    return 1;
                })
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "info", String.valueOf(IntegerArgumentType.getInteger(ctx, "page")));
                        return 1;
                    }))
                   );

        swNode.then(Commands.literal("reload").requires(s -> s.getSender().hasPermission("sparkword.reload"))
            .executes(ctx -> {
                manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "reload");
                return 1;
            }));
    }

    @Override
    public void register(Commands commands, CommandManager manager, Environment env, Map<String, LiteralCommandNode<CommandSourceStack>> registry) {
        var debugBuilder = Commands.literal("sw-debug")
            .requires(s -> s.getSender().hasPermission("sparkword.debug"))
            .executes(ctx -> run(manager, ctx, "sw-debug"))
            .then(Commands.literal("filter").executes(ctx -> run(manager, ctx, "sw-debug", "filter")));

        LiteralCommandNode<CommandSourceStack> debugNode = debugBuilder.build();
        commands.register(debugNode, "Debug Mode", Collections.emptyList());
        registry.put("sw-debug", debugNode);

        var internalBuilder = Commands.literal("sw-internal")
            .requires(s -> s.getSender().hasPermission("sparkword.notify"))
            .executes(ctx -> run(manager, ctx, "sw-internal"))
            .then(Commands.literal("viewbook")
                    .executes(ctx -> run(manager, ctx, "sw-internal", "viewbook"))
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> run(manager, ctx, "sw-internal", "viewbook", StringArgumentType.getString(ctx, "id")))
                         )
                 );

        LiteralCommandNode<CommandSourceStack> internalNode = internalBuilder.build();
        commands.register(internalNode, "Internal", Collections.emptyList());
        registry.put("sw-internal", internalNode);
    }
}
