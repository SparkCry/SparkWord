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
import com.sparkword.storage.SQLiteConnectionPool;
import com.sparkword.util.BenchmarkReporter;
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

    @TempDir Path tempDir;
    private SQLiteConnectionPool pool;
    private SparkWord pluginMock;

    @BeforeEach
    void setup() throws Exception {
        pluginMock = mock(SparkWord.class);
        when(pluginMock.getDataFolder()).thenReturn(tempDir.toFile());

        pool = new SQLiteConnectionPool(pluginMock);

        try (Connection conn = pool.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sw_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "last_login LONG, " +
                    "ip_address VARCHAR(45))");

        }
    }

    @AfterEach
    void tearDown() {
        if (pool != null) pool.close();
    }

    @Test
    @DisplayName("Login Masivo: 200 Jugadores (Lectura + Escritura Concurrente)")
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
                try (Connection conn = pool.getConnection()) {

                    boolean exists = false;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT last_login FROM sw_players WHERE uuid = ?")) {
                        ps.setString(1, uuid);
                        exists = ps.executeQuery().next();
                    }

                    String sql = exists ?
                            "UPDATE sw_players SET last_login = ? WHERE uuid = ?" :
                            "INSERT INTO sw_players (last_login, ip_address, name, uuid) VALUES (?, ?, ?, ?)";

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, System.currentTimeMillis());
                        if (exists) {
                            ps.setString(2, uuid);
                        } else {
                            ps.setString(2, "127.0.0.1");
                            ps.setString(3, name);
                            ps.setString(4, uuid);
                        }
                        ps.executeUpdate();
                    }
                    success.incrementAndGet();

                } catch (Exception e) {
                    errors.incrementAndGet();

                }
            }, loginExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long duration = System.currentTimeMillis() - start;

        loginExecutor.shutdown();

        BenchmarkReporter.log("RealLoginStress", "total_logins", playerCount, "players");
        BenchmarkReporter.log("RealLoginStress", "duration", duration, "ms");
        BenchmarkReporter.log("RealLoginStress", "throughput", (playerCount / (double)duration) * 1000, "logins/sec");
        BenchmarkReporter.log("RealLoginStress", "errors", errors.get(), "count");

        if (errors.get() > 0) {
            BenchmarkReporter.alert("RealLoginStress", "Se detectaron errores de conexión (Database Locked?)");
        }
    }
}
