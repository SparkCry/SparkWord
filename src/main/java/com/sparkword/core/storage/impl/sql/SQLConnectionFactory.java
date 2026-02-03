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
package com.sparkword.core.storage.impl.sql;

import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.config.StorageSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class SQLConnectionFactory {

    private final SparkWord plugin;
    private HikariDataSource dataSource;

    public SQLConnectionFactory(SparkWord plugin) {
        this.plugin = plugin;
    }

    public void init(ConfigManager cfg) {
        StorageSettings settings = cfg.getStorageSettings();
        String type = settings.getStorageType();

        try {
            if (type.equalsIgnoreCase("mysql") || type.equalsIgnoreCase("mariadb")) {
                initMySQL(settings);
            } else {
                initSQLite();
            }

            try (Connection ignored = dataSource.getConnection()) {
                plugin.log("<white>Database connected successfully (<#09bbf5>" + type + "<white>).");
            }
        } catch (Exception e) {
            plugin.log("<red>CRITICAL: Database connection failed!");
            plugin.getLogger().severe("Could not connect to " + type + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initSQLite() {
        File file = new File(plugin.getDataFolder(), "database/data.db");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file.getPath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setPoolName("SparkWord-SQLite");

        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "3000");

        this.dataSource = new HikariDataSource(config);
    }

    private void initMySQL(StorageSettings settings) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:mysql://" + settings.getDbHost() + ":" + settings.getDbPort() + "/" + settings.getDbName());
        config.setUsername(settings.getDbUser());
        config.setPassword(settings.getDbPassword());

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("MySQL driver (com.mysql.cj.jdbc.Driver) not found. Trying legacy or MariaDB...");
            try {
                Class.forName("org.mariadb.jdbc.Driver");
                config.setDriverClassName("org.mariadb.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                plugin.getLogger().severe("No suitable JDBC driver found for MySQL/MariaDB!");
            }
        }

        config.setMaximumPoolSize(settings.getDbPoolSize());
        config.setMaxLifetime(settings.getDbMaxLifetime());
        config.setConnectionTimeout(settings.getDbTimeout());
        config.setPoolName("SparkWord-MySQL");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource not initialized or failed to initialize");
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                plugin.getLogger().warning("Error closing DataSource: " + e.getMessage());
            }
        }
    }
}
