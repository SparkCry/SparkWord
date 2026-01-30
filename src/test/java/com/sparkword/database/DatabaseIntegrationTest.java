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
package com.sparkword.database;

import com.sparkword.core.storage.impl.sql.SQLConnectionFactory;
import com.sparkword.core.storage.impl.sql.SchemaManager;
import com.sparkword.core.storage.impl.sql.query.SQLiteQueryAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DatabaseIntegrationTest {

    private HikariDataSource dataSource;
    private SchemaManager schemaManager;

    @Mock
    private SQLConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite::memory:");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("Test-Integration-Pool");
        config.setMaximumPoolSize(1);

        dataSource = new HikariDataSource(config);

        when(connectionFactory.getConnection()).thenAnswer(i -> dataSource.getConnection());

        schemaManager = new SchemaManager(connectionFactory, Logger.getGlobal(), new SQLiteQueryAdapter());
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Verify correct table creation (Schema Integrity)")
    void testSchemaCreation() throws Exception {

        schemaManager.runMigrations();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
            Set<String> tables = new HashSet<>();
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }

            assertTrue(tables.contains("players"), "Missing table 'players'");
            assertTrue(tables.contains("muted"), "Missing table 'muted'");
            assertTrue(tables.contains("warnings"), "Missing table 'warnings'");
            assertTrue(tables.contains("suggestions"), "Missing table 'suggestions'");
            assertTrue(tables.contains("audit"), "Missing table 'audit'");
            assertTrue(tables.contains("monitor_logs"), "Missing table 'monitor_logs'");

            ResultSet cols = stmt.executeQuery("PRAGMA table_info(muted)");
            boolean hasExpiresAt = false;
            while (cols.next()) {
                if (cols.getString("name").equals("expires_at")) hasExpiresAt = true;
            }
            assertTrue(hasExpiresAt, "Table 'muted' is missing column 'expires_at'");
        }
    }
}
