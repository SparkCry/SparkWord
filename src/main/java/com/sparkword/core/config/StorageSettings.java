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
package com.sparkword.core.config;

import org.bukkit.configuration.file.FileConfiguration;

public class StorageSettings {
    private String storageType;
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUser;
    private String dbPassword;
    private int dbPoolSize;
    private long dbMaxLifetime;
    private int dbTimeout;

    public StorageSettings() {
    }

    public void load(FileConfiguration config) {
        this.storageType = config.getString("storage.type", "sqlite");
        this.dbHost = config.getString("storage.host", "localhost");
        this.dbPort = config.getInt("storage.port", 3306);
        this.dbName = config.getString("storage.database", "sparkword");
        this.dbUser = config.getString("storage.username", "root");
        this.dbPassword = config.getString("storage.password", "password");
        this.dbPoolSize = config.getInt("storage.pool-settings.maximum-pool-size", 10);
        this.dbMaxLifetime = config.getLong("storage.pool-settings.max-lifetime", 1800000L);
        this.dbTimeout = config.getInt("storage.pool-settings.connection-timeout", 5000);
    }

    public String getStorageType() {
        return storageType;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public int getDbPoolSize() {
        return dbPoolSize;
    }

    public long getDbMaxLifetime() {
        return dbMaxLifetime;
    }

    public int getDbTimeout() {
        return dbTimeout;
    }
}
