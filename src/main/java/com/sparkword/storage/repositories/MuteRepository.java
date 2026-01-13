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

import com.sparkword.model.MuteInfo;
import com.sparkword.model.MuteInfo.MuteScope;
import com.sparkword.storage.SQLiteConnectionPool;
import com.sparkword.storage.executor.StorageExecutors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class MuteRepository {

    private final SQLiteConnectionPool pool;
    private final StorageExecutors executors;

    private static final String INSERT_MUTE = "INSERT OR REPLACE INTO muted (player_id, reason, muted_by, expires_at, created_at, scope) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String DELETE_MUTE = "DELETE FROM muted WHERE player_id = ?";
    private static final String SELECT_MUTE = "SELECT reason, muted_by, expires_at, scope FROM muted WHERE player_id = ?";

    public MuteRepository(SQLiteConnectionPool pool, StorageExecutors executors) {
        this.pool = pool;
        this.executors = executors;
    }

    public CompletableFuture<Void> muteAsync(int playerId, String reason, String by, long durationSeconds, MuteScope scope) {

        return CompletableFuture.runAsync(() -> {
            long expires = durationSeconds > 0 ? System.currentTimeMillis() + (durationSeconds * 1000) : 0;
            try (Connection conn = pool.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_MUTE)) {
                ps.setInt(1, playerId);
                ps.setString(2, reason);
                ps.setString(3, by);
                ps.setLong(4, expires);
                ps.setLong(5, System.currentTimeMillis());
                ps.setString(6, scope.name());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executors.writer);
    }

    public CompletableFuture<Void> unmuteAsync(int playerId) {

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = pool.getConnection();
                 PreparedStatement ps = conn.prepareStatement(DELETE_MUTE)) {
                ps.setInt(1, playerId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executors.writer);
    }

    public MuteInfo fetchMuteInfoBlocking(int playerId) {
        try (Connection conn = pool.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_MUTE)) {
            ps.setInt(1, playerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long expires = rs.getLong("expires_at");

                if (expires != 0 && expires <= System.currentTimeMillis()) {

                    CompletableFuture.runAsync(() -> {
                        try (PreparedStatement del = conn.prepareStatement(DELETE_MUTE)) {
                            del.setInt(1, playerId);
                            del.executeUpdate();
                        } catch (SQLException e) { e.printStackTrace(); }
                    }, executors.writer);

                    return MuteInfo.NOT_MUTED;
                }

                String scopeStr = rs.getString("scope");
                MuteScope scope = MuteScope.CHAT;
                try {
                    if (scopeStr != null) scope = MuteScope.valueOf(scopeStr);
                } catch (IllegalArgumentException e) {}

                return new MuteInfo(true, rs.getString("muted_by"), rs.getString("reason"), expires, scope);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return MuteInfo.NOT_MUTED;
    }

    public CompletableFuture<Long> getMuteExpiryAsync(int playerId) {

        return CompletableFuture.supplyAsync(() -> {
            MuteInfo info = fetchMuteInfoBlocking(playerId);
            return info.isMuted() ? info.expiry() : -1L;
        }, executors.reader);
    }
}
