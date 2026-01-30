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
import com.sparkword.core.storage.spi.dao.WarningDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class SQLWarningDAO extends AbstractSQLDAO implements WarningDAO {

    private static final String INSERT_WARNING = "INSERT INTO warnings (player_id, moderator, reason, created_at) VALUES (?, ?, ?, ?)";

    public SQLWarningDAO(SQLConnectionFactory connectionFactory, ExecutorService writer, ExecutorService reader) {
        super(connectionFactory, writer, reader);
    }

    @Override
    public CompletableFuture<Void> addWarningAsync(int playerId, String reason, String moderator) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_WARNING)) {
                ps.setInt(1, playerId);
                ps.setString(2, moderator);
                ps.setString(3, reason);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, writer);
    }
}
