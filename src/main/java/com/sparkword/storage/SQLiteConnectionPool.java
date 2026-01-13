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
package com.sparkword.storage;

import com.sparkword.SparkWord;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class SQLiteConnectionPool {

    private final SparkWord plugin;
    private final HikariDataSource dataSource;

    public SQLiteConnectionPool(SparkWord plugin) {
        this.plugin = plugin;
        this.dataSource = setupPool("data.db");

        try (Connection ignored = dataSource.getConnection()) {
            plugin.log("<white>Database loaded successfully.");
        } catch (SQLException e) {
            plugin.log("<red>The database could not be loaded.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    private HikariDataSource setupPool(String fileName) {
        File file = new File(plugin.getDataFolder(), "database/" + fileName);
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file.getPath());
        config.setDriverClassName("org.sqlite.JDBC");

        config.setMaximumPoolSize(4);
        config.setMinimumIdle(2);

        config.setMaxLifetime(1_800_000);
        config.setConnectionTimeout(5_000);
        config.setPoolName("SparkWord-Pool");

        config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=3000; PRAGMA foreign_keys=ON;");

        return new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
