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
package com.sparkword.storage.repositories;

import com.sparkword.storage.SQLiteConnectionPool;
import com.sparkword.storage.executor.StorageExecutors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class WarningRepository {

    private final SQLiteConnectionPool pool;
    private final StorageExecutors executors;

    private static final String INSERT_WARNING = "INSERT INTO warnings (player_id, moderator, reason, created_at) VALUES (?, ?, ?, ?)";

    public WarningRepository(SQLiteConnectionPool pool, StorageExecutors executors) {
        this.pool = pool;
        this.executors = executors;
    }

    public CompletableFuture<Void> addWarningAsync(int playerId, String reason, String moderator) {

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = pool.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_WARNING)) {
                ps.setInt(1, playerId);
                ps.setString(2, moderator);
                ps.setString(3, reason);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executors.writer);
    }
}
