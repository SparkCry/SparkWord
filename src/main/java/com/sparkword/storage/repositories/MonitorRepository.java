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

public class MonitorRepository {

    private final SQLiteConnectionPool pool;
    private final StorageExecutors executors;

    private static final String INSERT_LOG = "INSERT INTO monitor_logs (player_name, content, category, source, detected_word, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String PURGE_LOGS = "DELETE FROM monitor_logs WHERE timestamp < ?";

    public MonitorRepository(SQLiteConnectionPool pool, StorageExecutors executors) {
        this.pool = pool;
        this.executors = executors;
    }

    public void addLogAsync(String playerName, String content, String category, String source, String detectedWord) {

        CompletableFuture.runAsync(() -> {
            try (Connection conn = pool.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_LOG)) {
                ps.setString(1, playerName);
                ps.setString(2, content);
                ps.setString(3, category);
                ps.setString(4, source);
                ps.setString(5, detectedWord);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executors.writer);
    }

    public CompletableFuture<Integer> purgeAsync(long daysOld) {

        return CompletableFuture.supplyAsync(() -> {
            long timeLimit = System.currentTimeMillis() - (daysOld * 86400000L);
            try (Connection conn = pool.getConnection();
                 PreparedStatement ps = conn.prepareStatement(PURGE_LOGS)) {
                ps.setLong(1, timeLimit);
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        }, executors.writer);
    }
}
