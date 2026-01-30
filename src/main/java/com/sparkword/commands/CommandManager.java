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
package com.sparkword.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sparkword.Environment;
import com.sparkword.SparkWord;
import com.sparkword.commands.brigadier.AliasHandler;
import com.sparkword.commands.brigadier.tree.*;
import com.sparkword.commands.impl.filter.FilterAddCommand;
import com.sparkword.commands.impl.filter.FilterListCommand;
import com.sparkword.commands.impl.filter.FilterRemoveCommand;
import com.sparkword.commands.impl.logs.AuditCommand;
import com.sparkword.commands.impl.logs.LogsCommand;
import com.sparkword.commands.impl.logs.PurgeCommand;
import com.sparkword.commands.impl.logs.ScanCommand;
import com.sparkword.commands.impl.mute.*;
import com.sparkword.commands.impl.root.DebugCommand;
import com.sparkword.commands.impl.root.InfoCommand;
import com.sparkword.commands.impl.root.InternalCommand;
import com.sparkword.commands.impl.root.ReloadCommand;
import com.sparkword.commands.impl.suggest.AcceptCommand;
import com.sparkword.commands.impl.suggest.DenyCommand;
import com.sparkword.commands.impl.suggest.SuggestCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

import java.util.*;

public class CommandManager {

    private final SparkWord plugin;
    private final Environment environment;
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final AliasHandler aliasHandler;

    // Store registered nodes to link aliases via redirection
    private final Map<String, LiteralCommandNode<CommandSourceStack>> nodeRegistry = new HashMap<>();

    public CommandManager(SparkWord plugin) {
        this.plugin = plugin;
        this.environment = plugin.getEnvironment();
        this.aliasHandler = new AliasHandler(plugin);

        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("info", new InfoCommand(environment));
        subCommands.put("debug", new DebugCommand(environment));
        subCommands.put("internal", new InternalCommand(environment));
        subCommands.put("reload", new ReloadCommand(environment));

        subCommands.put("mute", new MuteCommand(environment));
        subCommands.put("tempmute", new TempMuteCommand(environment));
        subCommands.put("checkmute", new CheckMuteCommand(environment));
        subCommands.put("unmute", new UnmuteCommand(environment));
        subCommands.put("warn", new WarnCommand(environment));
        subCommands.put("permute", new PermuteCommand(environment));
        subCommands.put("scan", new ScanCommand(environment));

        subCommands.put("audit", new AuditCommand(environment));
        subCommands.put("logs", new LogsCommand(environment));
        subCommands.put("purge", new PurgeCommand(environment));

        subCommands.put("add", new FilterAddCommand(environment));
        subCommands.put("remove", new FilterRemoveCommand(environment));
        subCommands.put("list", new FilterListCommand(environment));

        subCommands.put("accept", new AcceptCommand(environment));
        subCommands.put("deny", new DenyCommand(environment));
        subCommands.put("sg", new SuggestCommand(environment));

        // Internally map aliases to logic too, so console/plugins can use them via API dispatch
        subCommands.put("sw-mute", subCommands.get("mute"));
        subCommands.put("sw-tempmute", subCommands.get("tempmute"));
        subCommands.put("sw-checkmute", subCommands.get("checkmute"));
        subCommands.put("sw-unmute", subCommands.get("unmute"));
        subCommands.put("sw-warn", subCommands.get("warn"));
        subCommands.put("sw-permute", subCommands.get("permute"));
        subCommands.put("sw-scan", subCommands.get("scan"));
        subCommands.put("sw-sg", subCommands.get("sg"));
        subCommands.put("sw-debug", subCommands.get("debug"));
        subCommands.put("sw-internal", subCommands.get("internal"));
    }

    public void loadAliasMap() {
        aliasHandler.load();
    }

    public void executeFromLifecycle(CommandSourceStack stack, String aliasLabel, String[] args) {
        String realCommand = aliasHandler.resolve(aliasLabel);
        if (realCommand == null) return;
        executeLogic(stack.getSender(), realCommand, args);
    }

    // Changed to varargs to support direct calls with individual string arguments
    public boolean dispatchFromBrigadier(CommandSender sender, String commandLabel, String... args) {
        return executeLogic(sender, commandLabel, args);
    }

    private boolean executeLogic(CommandSender sender, String cmdName, String[] args) {
        cmdName = cmdName.toLowerCase();

        // Resolve alias recursively if needed
        String resolved = aliasHandler.resolve(cmdName);
        if (resolved != null) cmdName = resolved;

        if (cmdName.equals("sparkword") || cmdName.equals("sw")) {
            return handleMainCommand(sender, args);
        }

        SubCommand sub = subCommands.get(cmdName);
        if (sub != null) {
            return sub.execute(sender, args);
        }

        return false;
    }

    private boolean handleMainCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            subCommands.get("info").execute(sender, args);
            return true;
        }

        String subLabel = args[0].toLowerCase();
        SubCommand cmd = subCommands.get(subLabel);

        if (cmd != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return cmd.execute(sender, subArgs);
        }

        subCommands.get("info").execute(sender, args);
        return true;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerBrigadierTree(Commands commands) {
        nodeRegistry.clear();

        // Register individual command nodes and capture them in nodeRegistry
        new MuteTree().register(commands, this, environment, nodeRegistry);
        new LogTree().register(commands, this, environment, nodeRegistry);
        new SuggestionTree().register(commands, this, environment, nodeRegistry);
        new SystemTree().register(commands, this, environment, nodeRegistry);

        // Register main /sw tree and attach sub-branches
        var swNode = Commands.literal("sw")
            .requires(s -> s.getSender().hasPermission("sparkword.info") || s.getSender().hasPermission("sparkword.moderator"))
            .executes(ctx -> run(ctx, "sw"));

        SystemTree.attachSubCommands(swNode, this);
        FilterTree.attachSubCommands(swNode, this);
        LogTree.attachSubCommands(swNode, this);
        SuggestionTree.attachSubCommands(swNode, this);

        LiteralCommandNode<CommandSourceStack> mainNode = swNode.build();
        commands.register(mainNode, "Main command", List.of("sparkword"));
        nodeRegistry.put("sw", mainNode);
        nodeRegistry.put("sparkword", mainNode);

        registerDynamicAliases(commands);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerDynamicAliases(Commands commands) {
        for (Map.Entry<String, String> entry : aliasHandler.getAliasMap().entrySet()) {
            String alias = entry.getKey();
            String realCmd = entry.getValue();

            if (nodeRegistry.containsKey(realCmd)) {
                LiteralCommandNode<CommandSourceStack> targetNode = nodeRegistry.get(realCmd);

                // Instead of redirecting (which loses the root executor in some contexts),
                // we copy the structure.
                var aliasBuilder = Commands.literal(alias)
                    .requires(targetNode.getRequirement());

                // 1. Copy the executor (Handles /alias with no args)
                if (targetNode.getCommand() != null) {
                    aliasBuilder.executes(targetNode.getCommand());
                }

                // 2. Copy all child nodes (Handles arguments and autocomplete)
                for (CommandNode<CommandSourceStack> child : targetNode.getChildren()) {
                    aliasBuilder.then(child);
                }

                commands.register(
                    aliasBuilder.build(),
                    "SparkWord alias for " + realCmd,
                    Collections.emptyList()
                );
            }
            // Fallback for simple aliases not mapped to the tree registry
            else {
                commands.register(
                    Commands.literal(alias)
                        .requires(stack -> true)
                        .executes(ctx -> {
                            executeFromLifecycle(ctx.getSource(), alias, new String[0]);
                            return 1;
                        })
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String argString = StringArgumentType.getString(ctx, "args");
                                executeFromLifecycle(ctx.getSource(), alias, argString.split(" "));
                                return 1;
                            })
                        )
                        .build(),
                    "SparkWord dynamic alias"
                );
            }
        }
    }

    private int run(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String label, String... args) {
        dispatchFromBrigadier(ctx.getSource().getSender(), label, args);
        return 1;
    }
}
