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
package com.sparkword.core.storage.impl.sql.dao;

import com.sparkword.core.storage.impl.sql.SQLConnectionFactory;
import com.sparkword.core.storage.spi.dao.SuggestionDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class SQLSuggestionDAO extends AbstractSQLDAO implements SuggestionDAO {

    private static final String SELECT_DUPLICATE = "SELECT 1 FROM suggestions WHERE suggestion = ? AND status = 'pending'";
    private static final String INSERT_SUGGESTION = "INSERT INTO suggestions (player_id, suggestion, reason, created_at) VALUES (?, ?, ?, ?)";
    private static final String SELECT_AND_JOIN = "SELECT s.suggestion, p.uuid FROM suggestions s JOIN players p ON s.player_id = p.id WHERE s.id = ? AND s.status = 'pending'";
    private static final String UPDATE_STATUS = "UPDATE suggestions SET status = ? WHERE id = ?";
    private static final String PURGE_SUGGESTIONS = "DELETE FROM suggestions WHERE created_at < ?";

    public SQLSuggestionDAO(SQLConnectionFactory connectionFactory, ExecutorService writer, ExecutorService reader) {
        super(connectionFactory, writer, reader);
    }

    @Override
    public CompletableFuture<Boolean> addSuggestionAsync(int playerId, String word, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionFactory.getConnection()) {
                try (PreparedStatement check = conn.prepareStatement(SELECT_DUPLICATE)) {
                    check.setString(1, word);
                    if (check.executeQuery().next()) return false;
                }

                try (PreparedStatement ps = conn.prepareStatement(INSERT_SUGGESTION)) {
                    ps.setInt(1, playerId);
                    ps.setString(2, word);
                    ps.setString(3, reason);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, writer);
    }

    @Override
    public CompletableFuture<SuggestionInfo> processSuggestionAsync(int id, boolean accept) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionFactory.getConnection()) {
                SuggestionInfo info = null;

                try (PreparedStatement ps = conn.prepareStatement(SELECT_AND_JOIN)) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        info = new SuggestionInfo(
                            rs.getString("suggestion"),
                            UUID.fromString(rs.getString("uuid"))
                        );
                    }
                }

                if (info == null) return null;

                try (PreparedStatement update = conn.prepareStatement(UPDATE_STATUS)) {
                    update.setString(1, accept ? "accepted" : "rejected");
                    update.setInt(2, id);
                    update.executeUpdate();
                }

                return info;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }, writer);
    }

    @Override
    public CompletableFuture<Integer> purgeAsync(long daysOld) {
        return CompletableFuture.supplyAsync(() -> {
            long timeLimit = System.currentTimeMillis() - (daysOld * 86400000L);
            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(PURGE_SUGGESTIONS)) {
                ps.setLong(1, timeLimit);
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        }, writer);
    }
}
