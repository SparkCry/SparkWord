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
import com.sparkword.core.storage.spi.dao.MonitorDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class SQLMonitorDAO extends AbstractSQLDAO implements MonitorDAO {

    private static final String INSERT_LOG = "INSERT INTO monitor_logs (player_name, content, category, source, detected_word, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String PURGE_LOGS = "DELETE FROM monitor_logs WHERE timestamp < ?";

    public SQLMonitorDAO(SQLConnectionFactory connectionFactory, ExecutorService writer, ExecutorService reader) {
        super(connectionFactory, writer, reader);
    }

    @Override
    public void addLogAsync(String playerName, String content, String category, String source, String detectedWord) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionFactory.getConnection();
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
        }, writer);
    }

    @Override
    public CompletableFuture<Integer> purgeAsync(long daysOld) {
        return CompletableFuture.supplyAsync(() -> {
            long timeLimit = System.currentTimeMillis() - (daysOld * 86400000L);
            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(PURGE_LOGS)) {
                ps.setLong(1, timeLimit);
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        }, writer);
    }
}
