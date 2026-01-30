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
import com.sparkword.core.storage.impl.sql.query.MySQLQueryAdapter;
import com.sparkword.core.storage.impl.sql.query.QueryAdapter;
import com.sparkword.core.storage.spi.dao.PlayerDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class SQLPlayerDAO extends AbstractSQLDAO implements PlayerDAO {

    private static final String SELECT_ID = "SELECT id FROM players WHERE uuid = ?";
    private static final String SELECT_NAME = "SELECT name FROM players WHERE id = ?";
    private static final String SELECT_ID_BY_NAME = "SELECT id FROM players WHERE LOWER(name) = LOWER(?) LIMIT 1";
    private final QueryAdapter queryAdapter;

    public SQLPlayerDAO(SQLConnectionFactory connectionFactory, ExecutorService writer, ExecutorService reader, QueryAdapter queryAdapter) {
        super(connectionFactory, writer, reader);
        this.queryAdapter = queryAdapter;
    }

    @Override
    public int getPlayerIdBlocking(UUID uuid, String name) {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ID)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return upsertPlayer(uuid, name);
    }

    @Override
    public CompletableFuture<Integer> getPlayerIdAsync(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> getPlayerIdBlocking(uuid, name), writer);
    }

    @Override
    public CompletableFuture<String> getPlayerNameAsync(int id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(SELECT_NAME)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("name");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return "Unknown (ID: " + id + ")";
        }, reader);
    }

    @Override
    public CompletableFuture<Integer> getPlayerIdByNameAsync(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(SELECT_ID_BY_NAME)) {
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("id");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;
        }, reader);
    }

    private int upsertPlayer(UUID uuid, String name) {
        String sql = queryAdapter.getPlayerUpsertQuery();
        long now = System.currentTimeMillis();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (queryAdapter instanceof MySQLQueryAdapter) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setLong(3, now);
            } else {
                // SQLite
                ps.setString(1, uuid.toString());
                ps.setString(2, uuid.toString());
                ps.setString(3, name);
                ps.setLong(4, now);
            }

            ps.executeUpdate();

            try (PreparedStatement fetch = conn.prepareStatement(SELECT_ID)) {
                fetch.setString(1, uuid.toString());
                ResultSet rs = fetch.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to upsert player " + name + " (UUID: " + uuid + ")", e);
        }
        return -1;
    }
}
