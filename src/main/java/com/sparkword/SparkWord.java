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
package com.sparkword;

import com.sparkword.api.SparkWordAPI;
import com.sparkword.commands.CommandManager;
import com.sparkword.core.Environment;
import com.sparkword.filters.FilterManager;
import com.sparkword.listeners.*;
import com.sparkword.spammers.SpamManager;
import com.sparkword.util.UpdateChecker;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.papermc.paper.event.player.AsyncChatEvent;

public final class SparkWord extends JavaPlugin {

    private static SparkWord instance;
    private static final int BSTATS_ID = 28822;

    private Environment environment;
    private FilterManager filterManager;
    private SpamManager spamManager;
    private CommandManager commandManager;

    private ChatListener activeChatListener;
    private int purgeTaskId = -1;

    private volatile boolean debugMode = false;
    private volatile boolean debugFilter = false;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        silenceLibraries();

        printBanner();

        try {

            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            if (!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();
            if (!new File(getDataFolder(), "messages.yml").exists()) saveResource("messages.yml", false);
            if (!new File(getDataFolder(), "commands.yml").exists()) saveResource("commands.yml", false);

            try {
                this.environment = new Environment(this);
            } catch (Exception e) {
                log("<red>Critical failure initializing environment.");
                getLogger().log(Level.SEVERE, "Stacktrace:", e);
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            this.debugMode = getConfig().getBoolean("debug", false);

            this.spamManager = new SpamManager(this);
            this.filterManager = new FilterManager(this);
            this.filterManager.loadFilters().thenRun(() -> {
                if (debugMode) getLogger().info("Filters loaded.");
            });

            new SparkWordAPI(this);
            this.commandManager = new CommandManager(this);
            registerBrigadierHints();
            registerListeners();
            registerDynamicChatListener();
            registerIncomingChannels();

            startPurgeTask();

            Metrics metrics = new Metrics(this, BSTATS_ID);

            metrics.addCustomChart(new SimplePie("database_type", () -> "SQLite"));
            metrics.addCustomChart(new SimplePie("anti_flood_enabled", () ->
                environment.getConfigManager().isAntiFloodEnabled() ? "Yes" : "No"));

            UpdateChecker updater = new UpdateChecker(this);
            updater.check();
            getServer().getPluginManager().registerEvents(updater, this);

            long time = System.currentTimeMillis() - startTime;

            log("<white>Enabled successfully in <#09bbf5>" + time + "ms<white>.");

        } catch (Throwable t) {
            log("<red>CRITICAL ERROR starting SparkWord.");
            getLogger().log(Level.SEVERE, "", t);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        if (environment != null) {
            try {
                environment.shutdown();
            } catch (Exception e) {
                getLogger().warning("Error closing environment: " + e.getMessage());
            }
        }

        environment = null;
        spamManager = null;
        filterManager = null;
        instance = null;

        log("<white>Disabled successfully.");
    }

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(
            "<#09bbf5>[SparkWord]</#09bbf5> <reset>" + message
        ));
    }

    private void printBanner() {
        String[] banner = {
            "<#09bbf5> ___                 _    _ _ _              _   </#09bbf5>",
            "<#09bbf5>/ __> ___  ___  _ _ | |__| | | | ___  _ _  _| |  </#09bbf5>",
            "<#09bbf5>\\__ \\| . \\\\<_> || '_>| / /| | | |/ . \\| '_>/ . |  </#09bbf5>",
            "<#09bbf5><___/|  _/<___||_|  |_\\_\\|__/_/ \\___/|_|  \\___|  </#09bbf5>",
            "<#09bbf5>     |_|                                         </#09bbf5>"
        };

        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(line));
        }
    }

    private void silenceLibraries() {
        String[] packages = {
            "com.sparkword.libs.hikari",
            "com.sparkword.libs.hikari.HikariDataSource",
            "com.sparkword.libs.hikari.pool.HikariPool",
            "com.zaxxer.hikari"
        };

        try {
            Class<?> configurator = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> levelClazz = Class.forName("org.apache.logging.log4j.Level");
            Object levelWarn = levelClazz.getField("WARN").get(null);

            java.lang.reflect.Method setLevel = configurator.getMethod("setLevel", String.class, levelClazz);

            for (String pkg : packages) {
                setLevel.invoke(null, pkg, levelWarn);
            }
        } catch (Throwable ignored) {
            for (String pkg : packages) {
                Logger logger = Logger.getLogger(pkg);
                if (logger != null) logger.setLevel(Level.WARNING);
            }
        }
    }

    private void startPurgeTask() {
        if (environment == null || environment.getStorage() == null) return;
        long purgeHours = getConfig().getInt("suggestion.purge-hours", 72);
        if (purgeHours > 0) {
            var task = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> environment.getStorage().purgeData("sg", purgeHours / 24), 1200L, 72000L);
            this.purgeTaskId = task.getTaskId();
        }
    }

    private void registerIncomingChannels() {
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "minecraft:brand", new ClientBrandListener(this));
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerBrigadierHints() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            if (commandManager != null) {
                commandManager.registerBrigadierTree(commands);
            }
        });
    }

    public void reload() {
        try {
            reloadConfig();
            if (environment != null) environment.reload();
            if (spamManager != null) spamManager.reload();
            if (filterManager != null) filterManager.loadFilters();
            if (commandManager != null) commandManager.loadAliasMap();

            Bukkit.getScheduler().cancelTask(purgeTaskId);
            startPurgeTask();
            registerDynamicChatListener();

            debugMode = getConfig().getBoolean("debug", false);
        } catch (Exception e) {
            getLogger().severe("Error reloading SparkWord: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerDynamicChatListener() {
        if (activeChatListener != null) {
            AsyncChatEvent.getHandlerList().unregister(activeChatListener);
        }
        EventPriority priority = (environment != null) ? environment.getConfigManager().getEventPriority() : EventPriority.HIGH;
        activeChatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvent(AsyncChatEvent.class, activeChatListener, priority, (l, e) -> {
            if (e instanceof AsyncChatEvent ce) ((ChatListener) l).onPlayerChat(ce);
        }, this);
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ClientBrandListener(this), this);
        pm.registerEvents(new AnvilListener(this), this);
        pm.registerEvents(new BookListener(this), this);
        pm.registerEvents(new SignListener(this), this);
        pm.registerEvents(new CommandListener(this), this);
    }

    public static SparkWord getInstance() { return instance; }
    public Environment getEnvironment() { return environment; }
    public com.sparkword.core.SecurityManager getSecurityManager() { return environment != null ? environment.getSecurityManager() : null; }
    public FilterManager getFilterManager() { return filterManager; }
    public SpamManager getSpamManager() { return spamManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    public boolean isDebugFilter() { return debugFilter; }
    public void setDebugFilter(boolean debugFilter) { this.debugFilter = debugFilter; }
}
