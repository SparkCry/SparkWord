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

public class SQLiteQueryAdapter implements QueryAdapter {

    @Override
    public String getPlayerUpsertQuery() {
        return "INSERT OR REPLACE INTO players (id, uuid, name, last_seen) VALUES " +
            "((SELECT id FROM players WHERE uuid = ?), ?, ?, ?)";

    }

    @Override
    public String getMuteUpsertQuery() {
        return "INSERT OR REPLACE INTO muted (player_id, reason, muted_by, expires_at, created_at, scope) VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    public String getMuteHistoryInsertQuery() {
        return "INSERT INTO mute_history (player_id, reason, moderator, duration, created_at, scope) VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    public String getTableCreationQuery(String tableName) {

        return switch (tableName) {
            case "players" -> "CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid TEXT UNIQUE, " +
                "name TEXT, " +
                "last_seen INTEGER)";
            case "suggestion" -> "CREATE TABLE IF NOT EXISTS suggestions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_id INTEGER, " +
                "suggestion TEXT, " +
                "reason TEXT, " +
                "status TEXT DEFAULT 'pending', " +
                "created_at INTEGER, " +
                "FOREIGN KEY(player_id) REFERENCES players(id))";
            case "warnings" -> "CREATE TABLE IF NOT EXISTS warnings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_id INTEGER, " +
                "moderator TEXT, " +
                "reason TEXT, " +
                "created_at INTEGER, " +
                "FOREIGN KEY(player_id) REFERENCES players(id))";
            case "muted" -> "CREATE TABLE IF NOT EXISTS muted (" +
                "player_id INTEGER PRIMARY KEY, " +
                "reason TEXT, " +
                "muted_by TEXT, " +
                "expires_at INTEGER, " +
                "created_at INTEGER, " +
                "scope TEXT DEFAULT 'CHAT', " +
                "FOREIGN KEY(player_id) REFERENCES players(id))";
            case "mute_history" -> "CREATE TABLE IF NOT EXISTS mute_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_id INTEGER, " +
                "reason TEXT, " +
                "moderator TEXT, " +
                "duration INTEGER, " +
                "created_at INTEGER, " +
                "scope TEXT, " +
                "FOREIGN KEY(player_id) REFERENCES players(id))";
            case "audit" -> "CREATE TABLE IF NOT EXISTS audit (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "staff_name TEXT, " +
                "action TEXT, " +
                "detail TEXT, " +
                "timestamp INTEGER)";
            case "monitor_logs" -> "CREATE TABLE IF NOT EXISTS monitor_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_name TEXT, " +
                "content TEXT, " +
                "category TEXT, " +
                "source TEXT DEFAULT 'Unknown', " +
                "detected_word TEXT, " +
                "timestamp INTEGER)";
            default -> "";
        };
    }
}
