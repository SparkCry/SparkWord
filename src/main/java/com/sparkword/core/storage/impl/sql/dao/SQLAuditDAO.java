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
import com.sparkword.core.storage.model.AuditEntry;
import com.sparkword.core.storage.spi.dao.AuditDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class SQLAuditDAO extends AbstractSQLDAO implements AuditDAO {

    private static final String INSERT_AUDIT = "INSERT INTO audit (staff_name, action, detail, timestamp) VALUES (?, ?, ?, ?)";
    private static final String SELECT_AUDIT_ALL = "SELECT id, staff_name, action, detail, timestamp FROM audit ORDER BY timestamp DESC LIMIT ?";
    private static final String SELECT_AUDIT_BY_STAFF = "SELECT id, staff_name, action, detail, timestamp FROM audit WHERE staff_name LIKE ? ORDER BY timestamp DESC LIMIT ?";
    private static final String PURGE_AUDIT = "DELETE FROM audit WHERE timestamp < ?";

    public SQLAuditDAO(SQLConnectionFactory connectionFactory, ExecutorService writer, ExecutorService reader) {
        super(connectionFactory, writer, reader);
    }

    @Override
    public void logAuditAsync(String staffName, String action, String detail) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_AUDIT)) {
                ps.setString(1, staffName);
                ps.setString(2, action);
                ps.setString(3, detail);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, writer);
    }

    @Override
    public CompletableFuture<List<AuditEntry>> getAuditLogsStructAsync(String staffTarget, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<AuditEntry> list = new ArrayList<>();
            String sql = (staffTarget == null) ? SELECT_AUDIT_ALL : SELECT_AUDIT_BY_STAFF;

            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                if (staffTarget == null) {
                    ps.setInt(1, limit);
                } else {
                    ps.setString(1, staffTarget);
                    ps.setInt(2, limit);
                }

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new AuditEntry(
                        rs.getInt("id"),
                        rs.getString("staff_name"),
                        rs.getString("action"),
                        rs.getString("detail"),
                        rs.getLong("timestamp")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }, reader);
    }

    @Override
    public CompletableFuture<List<String>> getAuditLogsAsync(String staffTarget, int limit) {
        return getAuditLogsStructAsync(staffTarget, limit).thenApply(entries -> {
            List<String> strings = new ArrayList<>();
            for (AuditEntry e : entries) strings.add(e.toString());
            return strings;
        });
    }

    @Override
    public CompletableFuture<Integer> purgeAsync(long daysOld) {
        return CompletableFuture.supplyAsync(() -> {
            long timeLimit = System.currentTimeMillis() - (daysOld * 86400000L);
            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(PURGE_AUDIT)) {
                ps.setLong(1, timeLimit);
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        }, writer);
    }
}
