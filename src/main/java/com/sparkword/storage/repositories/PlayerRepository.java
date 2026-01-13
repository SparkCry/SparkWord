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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository {

    private final SQLiteConnectionPool pool;
    private final StorageExecutors executors;

    private static final String INSERT_PLAYER = "INSERT OR IGNORE INTO players (uuid, name, last_seen) VALUES (?, ?, ?)";
    private static final String UPDATE_PLAYER = "UPDATE players SET name = ?, last_seen = ? WHERE uuid = ?";
    private static final String SELECT_PLAYER_DATA = "SELECT id, name, last_seen FROM players WHERE uuid = ?";
    private static final String SELECT_PLAYER_NAME = "SELECT name FROM players WHERE id = ?";

    private static final long UPDATE_THRESHOLD = 300_000L;

    public PlayerRepository(SQLiteConnectionPool pool, StorageExecutors executors) {
        this.pool = pool;
        this.executors = executors;
    }

    public int getPlayerIdBlocking(UUID uuid, String name) {
        if (executors.isWriterThread()) {
            return getPlayerIdInternal(uuid, name);
        }
        return CompletableFuture.supplyAsync(() -> getPlayerIdInternal(uuid, name), executors.writer).join();
    }

    public CompletableFuture<Integer> getPlayerIdAsync(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> getPlayerIdInternal(uuid, name), executors.writer);
    }

    public CompletableFuture<String> getPlayerNameAsync(int id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = pool.getConnection();
                 PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER_NAME)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("name");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return "Unknown (ID: " + id + ")";
        }, executors.reader);
    }

    private int getPlayerIdInternal(UUID uuid, String name) {
        try (Connection conn = pool.getConnection()) {

            int existingId = -1;
            String existingName = null;
            long lastSeen = 0;

            try (PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER_DATA)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    existingId = rs.getInt("id");
                    existingName = rs.getString("name");
                    lastSeen = rs.getLong("last_seen");
                }
            }

            long now = System.currentTimeMillis();

            if (existingId != -1) {
                boolean nameChanged = !name.equals(existingName);
                boolean timeToUpdate = (now - lastSeen) > UPDATE_THRESHOLD;

                if (nameChanged || timeToUpdate) {
                    try (PreparedStatement ps = conn.prepareStatement(UPDATE_PLAYER)) {
                        ps.setString(1, name);
                        ps.setLong(2, now);
                        ps.setString(3, uuid.toString());
                        ps.executeUpdate();
                    }
                }
                return existingId;
            }

            try (PreparedStatement ps = conn.prepareStatement(INSERT_PLAYER)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setLong(3, now);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }

            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
