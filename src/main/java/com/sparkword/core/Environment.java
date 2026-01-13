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
package com.sparkword.core;

import com.sparkword.SparkWord;
import com.sparkword.filters.FilterManager;
import com.sparkword.spammers.SpamManager;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class Environment {

    private final SparkWord plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final NotifyManager notifyManager;
    private final PlayerDataManager playerDataManager;
    private final SecurityManager securityManager;
    private final SQLiteStorage storage;

    public Environment(SparkWord plugin) {
        this.plugin = plugin;

        this.configManager = new ConfigManager(plugin);
        this.messageManager = new MessageManager(plugin);
        this.storage = new SQLiteStorage(plugin);
        this.notifyManager = new NotifyManager(plugin);
        this.playerDataManager = new PlayerDataManager(plugin);
        this.securityManager = new SecurityManager(plugin);
    }

    public void reload() {
        configManager.reload();
        messageManager.reload();
        storage.reload();
        playerDataManager.invalidateAll();
    }

    public void shutdown() {
        if (playerDataManager != null) playerDataManager.invalidateAll();
        if (storage != null) storage.close();
    }

    public SparkWord getPlugin() { return plugin; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public NotifyManager getNotifyManager() { return notifyManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public SecurityManager getSecurityManager() { return securityManager; }
    public SQLiteStorage getStorage() { return storage; }

    public FilterManager getFilterManager() { return plugin.getFilterManager(); }
    public SpamManager getSpamManager() { return plugin.getSpamManager(); }

    public Executor getAsyncExecutor() {
        return ForkJoinPool.commonPool();
    }
}
