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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class SQLiteMigrations {

    private final SQLiteConnectionPool pool;
    private final Logger logger;

    public SQLiteMigrations(SQLiteConnectionPool pool, Logger logger) {
        this.pool = pool;
        this.logger = logger;
    }

    public void runMigrations() {
        try (Connection conn = pool.getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid TEXT UNIQUE, " +
                    "name TEXT, " +
                    "last_seen INTEGER);");

            stmt.execute("CREATE TABLE IF NOT EXISTS suggestions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_id INTEGER, " +
                    "suggestion TEXT, " +
                    "reason TEXT, " +
                    "status TEXT DEFAULT 'pending', " +
                    "created_at INTEGER, " +
                    "FOREIGN KEY(player_id) REFERENCES players(id));");

            stmt.execute("CREATE TABLE IF NOT EXISTS warnings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_id INTEGER, " +
                    "moderator TEXT, " +
                    "reason TEXT, " +
                    "created_at INTEGER, " +
                    "FOREIGN KEY(player_id) REFERENCES players(id));");

            stmt.execute("CREATE TABLE IF NOT EXISTS muted (" +
                    "player_id INTEGER PRIMARY KEY, " +
                    "reason TEXT, " +
                    "muted_by TEXT, " +
                    "expires_at INTEGER, " +
                    "created_at INTEGER, " +
                    "scope TEXT DEFAULT 'CHAT', " +
                    "FOREIGN KEY(player_id) REFERENCES players(id));");

            stmt.execute("CREATE TABLE IF NOT EXISTS audit (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "staff_name TEXT, " +
                    "action TEXT, " +
                    "detail TEXT, " +
                    "timestamp INTEGER);");

            stmt.execute("CREATE TABLE IF NOT EXISTS monitor_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_name TEXT, " +
                    "content TEXT, " +
                    "category TEXT, " +
                    "timestamp INTEGER);");

            try {
                stmt.execute("ALTER TABLE monitor_logs ADD COLUMN source TEXT DEFAULT 'Unknown'");
            } catch (SQLException ignored) {}

            try {
                stmt.execute("ALTER TABLE monitor_logs ADD COLUMN detected_word TEXT");
            } catch (SQLException ignored) {}

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON monitor_logs(timestamp);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_warnings_player ON warnings(player_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_uuid ON players(uuid);");

        } catch (SQLException e) {
            logger.severe("Error executing SQLite migrations: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
