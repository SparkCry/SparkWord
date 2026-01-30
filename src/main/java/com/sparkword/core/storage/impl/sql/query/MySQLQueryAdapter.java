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
package com.sparkword.core.storage.impl.sql.query;

public class MySQLQueryAdapter implements QueryAdapter {

    @Override
    public String getPlayerUpsertQuery() {
        // MySQL uses standard ordered parameters: UUID, Name, LastSeen
        return "INSERT INTO players (uuid, name, last_seen) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name = VALUES(name), last_seen = VALUES(last_seen)";
    }

    @Override
    public String getMuteUpsertQuery() {
        return "INSERT INTO muted (player_id, reason, muted_by, expires_at, created_at, scope) VALUES (?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE reason = VALUES(reason), muted_by = VALUES(muted_by), " +
            "expires_at = VALUES(expires_at), created_at = VALUES(created_at), scope = VALUES(scope)";
    }

    @Override
    public String getTableCreationQuery(String tableName) {
        return switch (tableName) {
            case "players" -> "CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "uuid VARCHAR(36) UNIQUE, " +
                "name VARCHAR(64), " + // Increased to 64 to prevent truncation
                "last_seen BIGINT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            case "suggestion" -> "CREATE TABLE IF NOT EXISTS suggestions (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "player_id INTEGER, " +
                "suggestion TEXT, " +
                "reason TEXT, " +
                "status VARCHAR(32) DEFAULT 'pending', " +
                "created_at BIGINT, " +
                "FOREIGN KEY(player_id) REFERENCES players(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            case "warnings" -> "CREATE TABLE IF NOT EXISTS warnings (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "player_id INTEGER, " +
                "moderator VARCHAR(64), " +
                "reason TEXT, " +
                "created_at BIGINT, " +
                "FOREIGN KEY(player_id) REFERENCES players(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            case "muted" -> "CREATE TABLE IF NOT EXISTS muted (" +
                "player_id INTEGER PRIMARY KEY, " +
                "reason TEXT, " +
                "muted_by VARCHAR(64), " +
                "expires_at BIGINT, " +
                "created_at BIGINT, " +
                "scope VARCHAR(32) DEFAULT 'CHAT', " +
                "FOREIGN KEY(player_id) REFERENCES players(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            case "audit" -> "CREATE TABLE IF NOT EXISTS audit (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "staff_name VARCHAR(64), " +
                "action VARCHAR(64), " +
                "detail TEXT, " +
                "timestamp BIGINT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            case "monitor_logs" -> "CREATE TABLE IF NOT EXISTS monitor_logs (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "player_name VARCHAR(64), " +
                "content TEXT, " +
                "category VARCHAR(64), " +
                "source VARCHAR(64) DEFAULT 'Unknown', " +
                "detected_word VARCHAR(128), " +
                "timestamp BIGINT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            default -> "";
        };
    }
}
