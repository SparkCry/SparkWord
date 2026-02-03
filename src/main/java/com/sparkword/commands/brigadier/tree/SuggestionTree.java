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

import java.util.List;
import java.util.Map;

public class SuggestionTree implements BrigadierNode {

    public static void attachSubCommands(LiteralArgumentBuilder<CommandSourceStack> swNode, CommandManager manager) {
        swNode.then(Commands.literal("accept").requires(s -> s.getSender().hasPermission("sparkword.accept"))
                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "accept");
                    return 1;
                })
                .then(Commands.argument("id", IntegerArgumentType.integer())
                        .then(Commands.argument("list", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    b.suggest("n");
                                    b.suggest("s");
                                    b.suggest("wc");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "accept", String.valueOf(IntegerArgumentType.getInteger(ctx, "id")), StringArgumentType.getString(ctx, "list"));
                                    return 1;
                                })
                             )
                     )
                   );

        swNode.then(Commands.literal("deny").requires(s -> s.getSender().hasPermission("sparkword.deny"))
                .executes(ctx -> {
                    manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "deny");
                    return 1;
                })
                .then(Commands.argument("id", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            manager.dispatchFromBrigadier(ctx.getSource().getSender(), "sw", "deny", String.valueOf(IntegerArgumentType.getInteger(ctx, "id")));
                            return 1;
                        })
                     )
                   );
    }

    @Override
    public void register(Commands commands, CommandManager manager, Environment env, Map<String, LiteralCommandNode<CommandSourceStack>> registry) {
        var builder = Commands.literal("sw-sg")
            .requires(src -> src.getSender().hasPermission("sparkword.suggest"))
            .executes(ctx -> run(manager, ctx, "sw-sg"))
            .then(Commands.argument("word", StringArgumentType.word())
                    .executes(ctx -> run(manager, ctx, "sw-sg", StringArgumentType.getString(ctx, "word")))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> run(manager, ctx, "sw-sg", StringArgumentType.getString(ctx, "word"), StringArgumentType.getString(ctx, "reason")))
                         )
                 );

        LiteralCommandNode<CommandSourceStack> node = builder.build();
        commands.register(node, "Suggest word", List.of("suggest"));
        registry.put("sw-sg", node);
    }
}
