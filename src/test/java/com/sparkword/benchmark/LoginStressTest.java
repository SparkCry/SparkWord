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
package com.sparkword.benchmark;

import com.sparkword.SparkWord;
import com.sparkword.util.BenchmarkReporter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginStressTest {

    @TempDir
    Path tempDir;
    private HikariDataSource dataSource;
    private SparkWord pluginMock;

    @BeforeEach
    void setup() throws Exception {
        pluginMock = mock(SparkWord.class);
        when(pluginMock.getDataFolder()).thenReturn(tempDir.toFile());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + tempDir.resolve("stress.db"));
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(4);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");

            stmt.execute("CREATE TABLE IF NOT EXISTS sw_players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16), " +
                "last_login LONG, " +
                "ip_address VARCHAR(45))");
        }
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) dataSource.close();
    }

    @Test
    @DisplayName("Mass Login: 200 Players (Concurrent UPSERT)")
    void testRealLoginConcurrency() {
        int playerCount = 200;
        ExecutorService loginExecutor = Executors.newFixedThreadPool(50);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        long start = System.currentTimeMillis();

        for (int i = 0; i < playerCount; i++) {
            final String uuid = UUID.randomUUID().toString();
            final String name = "Player_" + i;

            futures.add(CompletableFuture.runAsync(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    String sql = "INSERT OR REPLACE INTO sw_players (uuid, name, last_login, ip_address) VALUES (?, ?, ?, ?)";

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, uuid);
                        ps.setString(2, name);
                        ps.setLong(3, System.currentTimeMillis());
                        ps.setString(4, "127.0.0.1");
                        ps.executeUpdate();
                    }
                    success.incrementAndGet();

                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                }
            }, loginExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long duration = System.currentTimeMillis() - start;

        loginExecutor.shutdown();

        BenchmarkReporter.log("RealLoginStress", "total_logins", playerCount, "players");
        BenchmarkReporter.log("RealLoginStress", "duration", duration, "ms");
        BenchmarkReporter.log("RealLoginStress", "throughput", (playerCount / (double) duration) * 1000, "logins/sec");
        BenchmarkReporter.log("RealLoginStress", "errors", errors.get(), "count");

        if (errors.get() > 0) {
            BenchmarkReporter.alert("RealLoginStress", "Connection errors detected (Database Locked?)");
        }
    }
}
