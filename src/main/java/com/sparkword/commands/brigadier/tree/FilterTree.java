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
import com.sparkword.commands.CommandManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class FilterTree {

    public static void attachSubCommands(LiteralArgumentBuilder<CommandSourceStack> swNode, CommandManager manager) {
        swNode.then(Commands.literal("add").requires(s -> s.getSender().hasPermission("sparkword.add"))
                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "add");
                    return 1;
                })
                .then(Commands.argument("list", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            b.suggest("n");
                            b.suggest("s");
                            b.suggest("wc");
                            return b.buildFuture();
                        })
                        .executes(ctx -> {
                            manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "add", StringArgumentType.getString(ctx, "list"));
                            return 1;
                        })
                        .then(Commands.argument("word", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "add", StringArgumentType.getString(ctx, "list"), StringArgumentType.getString(ctx, "word"));
                                    return 1;
                                })
                             )
                     )
                   );

        swNode.then(Commands.literal("remove").requires(s -> s.getSender().hasPermission("sparkword.remove"))
                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "remove");
                    return 1;
                })
                .then(Commands.argument("list", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            b.suggest("n");
                            b.suggest("s");
                            b.suggest("wc");
                            return b.buildFuture();
                        })
                        .executes(ctx -> {
                            manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "remove", StringArgumentType.getString(ctx, "list"));
                            return 1;
                        })
                        .then(Commands.argument("word", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "remove", StringArgumentType.getString(ctx, "list"), StringArgumentType.getString(ctx, "word"));
                                    return 1;
                                })
                             )
                     )
                   );

        swNode.then(Commands.literal("list").requires(s -> s.getSender().hasPermission("sparkword.list"))
                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "list");
                    return 1;
                })
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            b.suggest("n");
                            b.suggest("s");
                            b.suggest("wc");
                            b.suggest("sg");
                            return b.buildFuture();
                        })
                        .executes(ctx -> {
                            manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "list", StringArgumentType.getString(ctx, "type"));
                            return 1;
                        })
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "list", StringArgumentType.getString(ctx, "type"), String.valueOf(IntegerArgumentType.getInteger(ctx, "page")));
                                return 1;
                            }))
                     )
                   );
    }
}
