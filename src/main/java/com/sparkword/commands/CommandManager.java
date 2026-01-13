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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sparkword.SparkWord;
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
import com.sparkword.core.Environment;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class CommandManager {

    private final SparkWord plugin;
    private final Environment environment;
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final Map<String, String> aliasToRealMap = new HashMap<>();

    public CommandManager(SparkWord plugin) {
        this.plugin = plugin;
        this.environment = plugin.getEnvironment();
        registerSubCommands();
        loadAliasMap();
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
        aliasToRealMap.clear();
        File file = new File(plugin.getDataFolder(), "commands.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.isConfigurationSection("commands.alias")) {
            for (String realCmd : config.getConfigurationSection("commands.alias").getKeys(false)) {
                List<String> aliases = config.getStringList("commands.alias." + realCmd);
                for (String alias : aliases) aliasToRealMap.put(alias.toLowerCase(), realCmd.toLowerCase());
                String single = config.getString("commands.alias." + realCmd);
                if (single != null && aliases.isEmpty()) aliasToRealMap.put(single.toLowerCase(), realCmd.toLowerCase());
            }
        }
    }

    public void executeFromLifecycle(CommandSourceStack stack, String aliasLabel, String[] args) {
        String realCommand = aliasToRealMap.get(aliasLabel.toLowerCase());
        if (realCommand == null) return;
        executeLogic(stack.getSender(), realCommand, args);
    }

    public boolean dispatchFromBrigadier(CommandSender sender, String commandLabel, String[] args) {
        return executeLogic(sender, commandLabel, args);
    }

    private boolean executeLogic(CommandSender sender, String cmdName, String[] args) {
        cmdName = cmdName.toLowerCase();

        int depth = 0;
        while (aliasToRealMap.containsKey(cmdName) && depth++ < 5) {
            cmdName = aliasToRealMap.get(cmdName);
        }

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
        registerStaticBrigadier(commands);
        registerDynamicAliases(commands);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerDynamicAliases(Commands commands) {
        for (Map.Entry<String, String> entry : aliasToRealMap.entrySet()) {
            String alias = entry.getKey();
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

    @SuppressWarnings("UnstableApiUsage")
    private void registerStaticBrigadier(Commands commands) {

        commands.register(Commands.literal("sw-permute")
                .requires(src -> src.getSender().hasPermission("sparkword.permute"))
                .executes(ctx -> run(ctx, "sw-permute"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                    .executes(ctx -> run(ctx, "sw-permute", StringArgumentType.getString(ctx, "player")))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> {
                            if (plugin.getConfig().isConfigurationSection("presets")) {
                                plugin.getConfig().getConfigurationSection("presets").getKeys(false).forEach(b::suggest);
                            }
                            return b.buildFuture();
                        })
                        .executes(ctx -> run(ctx, "sw-permute", StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "reason")))
                    )
                ).build(),
            "Extreme permanent mute",
            List.of()
        );

        commands.register(Commands.literal("sw-mute")
                .requires(src -> src.getSender().hasPermission("sparkword.mute"))
                .executes(ctx -> run(ctx, "sw-mute"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                    .executes(ctx -> run(ctx, "sw-mute", StringArgumentType.getString(ctx, "player")))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> {
                            if (plugin.getConfig().isConfigurationSection("presets")) {
                                plugin.getConfig().getConfigurationSection("presets").getKeys(false).forEach(b::suggest);
                            }
                            return b.buildFuture();
                        })
                        .executes(ctx -> run(ctx, "sw-mute", StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "reason")))
                    )
                ).build(),
            "Permanently mute",
            List.of("mute")
        );

        commands.register(Commands.literal("sw-warn")
                .requires(src -> src.getSender().hasPermission("sparkword.warn"))
                .executes(ctx -> run(ctx, "sw-warn"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                    .executes(ctx -> run(ctx, "sw-warn", StringArgumentType.getString(ctx, "player")))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, "sw-warn", StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "reason")))
                    )
                ).build(),
            "Warn a player",
            List.of("warn")
        );

        commands.register(Commands.literal("sw-tempmute")
                .requires(src -> src.getSender().hasPermission("sparkword.tempmute"))
                .executes(ctx -> run(ctx, "sw-tempmute"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                    .executes(ctx -> run(ctx, "sw-tempmute", StringArgumentType.getString(ctx, "player")))
                    .then(Commands.argument("time", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            b.suggest("15m"); b.suggest("30m"); b.suggest("45m");
                            b.suggest("1h"); b.suggest("3h"); b.suggest("6h"); b.suggest("12h"); b.suggest("24h");
                            b.suggest("1d"); b.suggest("3d"); b.suggest("7d"); b.suggest("30d");
                            return b.buildFuture();
                        })
                        .executes(ctx -> run(ctx, "sw-tempmute", StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "time")))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> run(ctx, "sw-tempmute", StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "time"), StringArgumentType.getString(ctx, "reason")))
                        )
                    )
                ).build(),
            "Temporarily mute",
            List.of("tempmute")
        );

        commands.register(Commands.literal("sw-checkmute")
            .requires(src -> src.getSender().hasPermission("sparkword.checkmute"))
            .executes(ctx -> run(ctx, "sw-checkmute"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw-checkmute", StringArgumentType.getString(ctx, "player")))
            ).build(), "Check mute", List.of("checkmute"));

        commands.register(Commands.literal("sw-unmute")
            .requires(src -> src.getSender().hasPermission("sparkword.unmute"))
            .executes(ctx -> run(ctx, "sw-unmute"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw-unmute", StringArgumentType.getString(ctx, "player")))
            ).build(), "Unmute", List.of("unmute"));

        commands.register(Commands.literal("sw-scan")
            .requires(src -> src.getSender().hasPermission("sparkword.scan"))
            .executes(ctx -> run(ctx, "sw-scan"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw-scan", StringArgumentType.getString(ctx, "player")))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> run(ctx, "sw-scan", StringArgumentType.getString(ctx, "player"), String.valueOf(IntegerArgumentType.getInteger(ctx, "page"))))
                )
            ).build(), "Player history", List.of("history"));

        commands.register(Commands.literal("sw-debug")
            .requires(s -> s.getSender().hasPermission("sparkword.debug"))
            .executes(ctx -> run(ctx, "sw-debug"))
            .then(Commands.literal("filter")
                .executes(ctx -> run(ctx, "sw-debug", "filter"))
            )
            .build(), "Debug Mode", List.of());

        commands.register(Commands.literal("sw-sg")
            .requires(src -> src.getSender().hasPermission("sparkword.suggest"))
            .executes(ctx -> run(ctx, "sw-sg"))
            .then(Commands.argument("word", StringArgumentType.word())
                .executes(ctx -> run(ctx, "sw-sg", StringArgumentType.getString(ctx, "word")))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> run(ctx, "sw-sg", StringArgumentType.getString(ctx, "word"), StringArgumentType.getString(ctx, "reason")))
                )
            ).build(), "Suggest banned word", List.of("suggest"));

        var swNode = Commands.literal("sw")
            .requires(s -> s.getSender().hasPermission("sparkword.info") || s.getSender().hasPermission("sparkword.moderator"))
            .executes(ctx -> run(ctx, "sw"));

        swNode.then(Commands.literal("info")
            .requires(s -> s.getSender().hasPermission("sparkword.info")) 
            .executes(ctx -> run(ctx, "sw", "info"))
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .suggests((ctx, b) -> { b.suggest(1); b.suggest(2); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw", "info", String.valueOf(IntegerArgumentType.getInteger(ctx, "page"))))
            )
        );

        swNode.then(Commands.literal("add").requires(s -> s.getSender().hasPermission("sparkword.add"))
            .executes(ctx -> run(ctx, "sw", "add"))
            .then(Commands.argument("list", StringArgumentType.word())
                .suggests((ctx, b) -> { b.suggest("n"); b.suggest("s"); b.suggest("wc"); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw", "add", StringArgumentType.getString(ctx, "list")))
                .then(Commands.argument("word", StringArgumentType.greedyString())
                    .executes(ctx -> run(ctx, "sw", "add", StringArgumentType.getString(ctx, "list"), StringArgumentType.getString(ctx, "word")))
                )
            )
        );

        swNode.then(Commands.literal("remove").requires(s -> s.getSender().hasPermission("sparkword.remove"))
            .executes(ctx -> run(ctx, "sw", "remove"))
            .then(Commands.argument("list", StringArgumentType.word())
                .suggests((ctx, b) -> { b.suggest("n"); b.suggest("s"); b.suggest("wc"); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw", "remove", StringArgumentType.getString(ctx, "list")))
                .then(Commands.argument("word", StringArgumentType.greedyString())
                    .executes(ctx -> run(ctx, "sw", "remove", StringArgumentType.getString(ctx, "list"), StringArgumentType.getString(ctx, "word")))
                )
            )
        );

        swNode.then(Commands.literal("audit").requires(s -> s.getSender().hasPermission("sparkword.audit"))
            .executes(ctx -> run(ctx, "sw", "audit"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw", "audit", StringArgumentType.getString(ctx, "player")))
            )
        );

        swNode.then(Commands.literal("list").requires(s -> s.getSender().hasPermission("sparkword.list"))
            .executes(ctx -> run(ctx, "sw", "list"))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, b) -> { b.suggest("n"); b.suggest("s"); b.suggest("wc"); b.suggest("sg"); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw", "list", StringArgumentType.getString(ctx, "type")))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> run(ctx, "sw", "list", StringArgumentType.getString(ctx, "type"), String.valueOf(IntegerArgumentType.getInteger(ctx, "page"))))
                )
            )
        );

        swNode.then(Commands.literal("accept").requires(s -> s.getSender().hasPermission("sparkword.accept"))
            .executes(ctx -> run(ctx, "sw", "accept"))
            .then(Commands.argument("id", IntegerArgumentType.integer())
                .executes(ctx -> run(ctx, "sw", "accept", String.valueOf(IntegerArgumentType.getInteger(ctx, "id"))))
                .then(Commands.argument("list", StringArgumentType.word())
                    .suggests((ctx, b) -> { b.suggest("n"); b.suggest("s"); b.suggest("wc"); return b.buildFuture(); })
                    .executes(ctx -> run(ctx, "sw", "accept", String.valueOf(IntegerArgumentType.getInteger(ctx, "id")), StringArgumentType.getString(ctx, "list")))
                )
            )
        );

        swNode.then(Commands.literal("deny").requires(s -> s.getSender().hasPermission("sparkword.deny"))
            .executes(ctx -> run(ctx, "sw", "deny"))
            .then(Commands.argument("id", IntegerArgumentType.integer())
                .executes(ctx -> run(ctx, "sw", "deny", String.valueOf(IntegerArgumentType.getInteger(ctx, "id"))))
            )
        );

        swNode.then(Commands.literal("logs").requires(s -> s.getSender().hasPermission("sparkword.logs"))
            .executes(ctx -> run(ctx, "sw", "logs"))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, b) -> { b.suggest("b"); b.suggest("m"); b.suggest("w"); b.suggest("all"); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw", "logs", StringArgumentType.getString(ctx, "type")))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> run(ctx, "sw", "logs", StringArgumentType.getString(ctx, "type"), String.valueOf(IntegerArgumentType.getInteger(ctx, "page"))))
                )
            )
        );

        swNode.then(Commands.literal("purge").requires(s -> s.getSender().hasPermission("sparkword.purge.logs"))
            .executes(ctx -> run(ctx, "sw", "purge"))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, b) -> { b.suggest("sg"); b.suggest("m"); b.suggest("w"); b.suggest("b"); b.suggest("a"); return b.buildFuture(); })
                .executes(ctx -> run(ctx, "sw", "purge", StringArgumentType.getString(ctx, "type")))
                .then(Commands.argument("days", IntegerArgumentType.integer(0))
                    .executes(ctx -> run(ctx, "sw", "purge", StringArgumentType.getString(ctx, "type"), String.valueOf(IntegerArgumentType.getInteger(ctx, "days"))))
                )
            )
        );

        swNode.then(Commands.literal("reload").requires(s -> s.getSender().hasPermission("sparkword.reload"))
            .executes(ctx -> run(ctx, "sw", "reload"))
        );

        swNode.then(Commands.literal("internal")
            .requires(s -> s.getSender().hasPermission("sparkword.notify"))
            .then(Commands.literal("viewbook")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> run(ctx, "sw", "internal", "viewbook", StringArgumentType.getString(ctx, "id")))
                )
            )
        );

        commands.register(swNode.build(), "Main command", List.of("sparkword"));
    }

    private int run(CommandContext<CommandSourceStack> ctx, String label, String... args) {
        dispatchFromBrigadier(ctx.getSource().getSender(), label, args);
        return 1;
    }
}
