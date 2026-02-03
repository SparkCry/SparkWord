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
import com.sparkword.core.storage.model.LogEntry;
import com.sparkword.core.storage.spi.dao.ReportDAO;
import com.sparkword.util.TimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class SQLReportDAO extends AbstractSQLDAO implements ReportDAO {

    public SQLReportDAO(SQLConnectionFactory connectionFactory, ExecutorService writer, ExecutorService reader) {
        super(connectionFactory, writer, reader);
    }

    @Override
    public CompletableFuture<List<LogEntry>> getGlobalLogsStructAsync(String type, int page) {
        return CompletableFuture.supplyAsync(() -> {
            List<LogEntry> list = new ArrayList<>();
            int offset = (page - 1) * 10;

            try (Connection conn = connectionFactory.getConnection()) {
                if (type.equals("b") || type.equals("all")) {
                    String sql = "SELECT player_name, source, category, content, detected_word, timestamp FROM monitor_logs ORDER BY timestamp DESC LIMIT 10 OFFSET ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, offset);
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            list.add(new LogEntry(
                                rs.getString("player_name"),
                                rs.getString("source") != null ? rs.getString("source") : "Unknown",
                                rs.getString("category"),
                                rs.getString("content"),
                                rs.getString("detected_word"),
                                rs.getLong("timestamp")
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }, reader);
    }

    @Override
    public CompletableFuture<List<String>> getPlayerScanReportAsync(int playerId, int page) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> report = new ArrayList<>();
            int limit = 10;
            int offset = (page - 1) * limit;

            long timeLimit = System.currentTimeMillis() - (365 * 86400000L);

            try (Connection conn = connectionFactory.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT reason, created_at, moderator FROM warnings WHERE player_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
                    ps.setInt(1, playerId);
                    ps.setInt(2, limit);
                    ps.setInt(3, offset);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        report.add("§e[WARN] §7" + TimeUtil.formatShortDate(rs.getLong("created_at")) +
                            " (" + rs.getString("moderator") + "): " + rs.getString("reason"));
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("SELECT reason, created_at, moderator, duration, scope FROM mute_history WHERE player_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
                    ps.setInt(1, playerId);
                    ps.setInt(2, limit);
                    ps.setInt(3, offset);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        long duration = rs.getLong("duration");
                        String timeStr = (duration == 0) ? "Perm" : TimeUtil.formatDuration(duration);
                        String prefix = "§c[MUTE]";

                        report.add(prefix + " §7" + TimeUtil.formatShortDate(rs.getLong("created_at")) +
                            " (" + rs.getString("moderator") + "): " + rs.getString("reason") + " §8[" + timeStr + "]");
                    }
                }

                if (page == 1) {
                    try (PreparedStatement ps = conn.prepareStatement("SELECT reason, expires_at FROM muted WHERE player_id = ?")) {
                        ps.setInt(1, playerId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            long exp = rs.getLong("expires_at");
                            String timeLeft = (exp == 0) ? "Perm" : TimeUtil.formatDuration((exp - System.currentTimeMillis()) / 1000);
                            report.add(0, "§c[ACTIVE STATUS] §7" + rs.getString("reason") + " | Expires: §e" + timeLeft);
                        }
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return report;
        }, reader);
    }

    @Override
    public CompletableFuture<List<String>> getPendingSuggestionsReportAsync(int page) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> results = new ArrayList<>();
            int offset = (page - 1) * 10;
            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id, suggestion, reason FROM suggestions WHERE status = 'pending' ORDER BY id ASC LIMIT 10 OFFSET ?")) {
                ps.setInt(1, offset);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    results.add("§7ID: §e" + rs.getInt("id") + " §f| §7" + rs.getString("suggestion") + " §7(" + rs.getString("reason") + ")");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return results;
        }, reader);
    }
}
