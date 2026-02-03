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
import com.sparkword.commands.brigadier.BrigadierUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Collections;
import java.util.Map;

public class LogTree implements BrigadierNode {

    public static void attachSubCommands(LiteralArgumentBuilder<CommandSourceStack> swNode, CommandManager manager) {
        swNode.then(Commands.literal("audit").requires(s -> s.getSender().hasPermission("sparkword.audit"))
                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "audit");
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(BrigadierUtils::suggestOnlinePlayers)
                        .executes(ctx -> {
                            manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "audit", StringArgumentType.getString(ctx, "player"));
                            return 1;
                        })
                     )
                   );

        swNode.then(Commands.literal("logs").requires(s -> s.getSender().hasPermission("sparkword.logs"))
                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "logs");
                    return 1;
                })
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            b.suggest("b");
                            b.suggest("m");
                            b.suggest("w");
                            b.suggest("all");
                            return b.buildFuture();
                        })
                        .executes(ctx -> {
                            manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "logs", StringArgumentType.getString(ctx, "type"));
                            return 1;
                        })
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "logs", StringArgumentType.getString(ctx, "type"), String.valueOf(IntegerArgumentType.getInteger(ctx, "page")));
                                return 1;
                            }))
                     )
                   );

        swNode.then(Commands.literal("purge").requires(s -> s.getSender().hasPermission("sparkword.purge.logs"))

                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "purge");
                    return 1;
                })
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            b.suggest("sg");
                            b.suggest("m");
                            b.suggest("w");
                            b.suggest("b");
                            b.suggest("a");
                            b.suggest("hp");
                            return b.buildFuture();
                        })
                        .then(Commands.argument("days", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "purge", StringArgumentType.getString(ctx, "type"), String.valueOf(IntegerArgumentType.getInteger(ctx, "days")));
                                return 1;
                            }))
                     )
                   );
    }

    @Override
    public void register(Commands commands, CommandManager manager, Environment env, Map<String, LiteralCommandNode<CommandSourceStack>> registry) {
        var builder = Commands.literal("sw-scan")
            .requires(src -> src.getSender().hasPermission("sparkword.scan"))
            .executes(ctx -> run(manager, ctx, "sw-scan"))
            .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(BrigadierUtils::suggestOnlinePlayers)
                    .executes(ctx -> run(manager, ctx, "sw-scan", StringArgumentType.getString(ctx, "player")))
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(ctx -> run(manager, ctx, "sw-scan", StringArgumentType.getString(ctx, "player"), String.valueOf(IntegerArgumentType.getInteger(ctx, "page"))))
                         )
                 );

        LiteralCommandNode<CommandSourceStack> node = builder.build();
        commands.register(node, "Player history", Collections.emptyList());
        registry.put("sw-scan", node);
    }
}
