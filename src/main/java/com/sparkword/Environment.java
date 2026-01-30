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

import com.sparkword.commands.CommandManager;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.MessageManager;
import com.sparkword.core.NotifyManager;
import com.sparkword.core.lifecycle.BootstrapIntegrations;
import com.sparkword.core.lifecycle.MaintenanceTasks;
import com.sparkword.core.lifecycle.RegisterListener;
import com.sparkword.core.storage.PlayerDataManager;
import com.sparkword.core.storage.StorageManager;
import com.sparkword.moderation.antispam.SpamManager;
import com.sparkword.moderation.filters.FilterManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class Environment {

    private final SparkWord plugin;

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final StorageManager storage;
    private final NotifyManager notifyManager;
    private final PlayerDataManager playerDataManager;
    private final MaintenanceTasks maintenanceTasks;
    private final BootstrapIntegrations integrations;
    private FilterManager filterManager;
    private SpamManager spamManager;
    private CommandManager commandManager;
    private RegisterListener registrarRef;

    public Environment(SparkWord plugin) {
        this.plugin = plugin;

        this.configManager = new ConfigManager(plugin);

        this.messageManager = new MessageManager(plugin, configManager);

        this.storage = new StorageManager(plugin, configManager);
        this.notifyManager = new NotifyManager(plugin);
        this.playerDataManager = new PlayerDataManager(plugin);

        this.maintenanceTasks = new MaintenanceTasks(plugin, storage);
        this.integrations = new BootstrapIntegrations(plugin);
    }

    public void load() {

        this.spamManager = new SpamManager(plugin);
        this.filterManager = new FilterManager(plugin);

        this.filterManager.loadFilters().thenRun(() -> {
            if (plugin.isDebugMode()) plugin.getLogger().info("Filters loaded.");
        });

        new SparkWordAPI(plugin);
        this.commandManager = new CommandManager(plugin);

        RegisterListener registrar = new RegisterListener(plugin, commandManager);

        registrar.registerBrigadierHints();
        registrar.registerStaticListeners();
        registrar.registerDynamicChatListener(configManager);
        registrar.registerIncomingChannels();

        maintenanceTasks.startPurgeTask();

        integrations.initMetrics(configManager);
        integrations.checkUpdates();

        this.registrarRef = registrar;
    }

    public void reload() {

        configManager.reload();
        messageManager.reload();
        storage.reload();

        // Invalidate current cache
        playerDataManager.invalidateAll();

        // Refresh all online players immediately to restore their state/mutes
        for (Player p : Bukkit.getOnlinePlayers()) {
            playerDataManager.refreshPlayer(p);
        }

        if (spamManager != null) spamManager.reload();
        if (filterManager != null) filterManager.loadFilters();
        if (commandManager != null) commandManager.loadAliasMap();

        maintenanceTasks.startPurgeTask();

        if (registrarRef != null) {
            registrarRef.registerDynamicChatListener(configManager);
        }

        plugin.setDebugMode(configManager.isDebugMode());
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
        maintenanceTasks.stopAll();

        if (playerDataManager != null) playerDataManager.invalidateAll();
        if (storage != null) storage.close();
    }

    public SparkWord getPlugin() {
        return plugin;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public NotifyManager getNotifyManager() {
        return notifyManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public StorageManager getStorage() {
        return storage;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }

    public SpamManager getSpamManager() {
        return spamManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public Executor getAsyncExecutor() {
        return ForkJoinPool.commonPool();
    }
}
