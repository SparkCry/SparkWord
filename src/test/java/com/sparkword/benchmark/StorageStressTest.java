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
import com.sparkword.storage.executor.StorageExecutors;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StorageStressTest {

    @TempDir Path tempDir;
    private SQLiteConnectionPool connectionPool;
    private StorageExecutors executors;
    private SparkWord pluginMock;

    @BeforeEach
    public void setup() throws Exception {
        pluginMock = mock(SparkWord.class);
        when(pluginMock.getDataFolder()).thenReturn(tempDir.toFile());

        connectionPool = new SQLiteConnectionPool(pluginMock);

        try (Connection conn = connectionPool.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");

            stmt.execute("CREATE TABLE IF NOT EXISTS stress_test (id INTEGER PRIMARY KEY AUTOINCREMENT, data TEXT, created_at LONG)");
        }

        executors = new StorageExecutors();
    }

    @AfterEach
    public void tearDown() {
        if (connectionPool != null) connectionPool.close();
        if (executors != null) executors.shutdown();
    }

    @Test
    @DisplayName("Benchmark: Solo Escrituras (Heavy Write Load)")
    public void testWriteOnlyPerformance() {
        int operations = 5_000;
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        long globalStart = System.currentTimeMillis();

        for (int i = 0; i < operations; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                long start = System.nanoTime();
                try (Connection conn = connectionPool.getConnection();
                     PreparedStatement ps = conn.prepareStatement("INSERT INTO stress_test (data, created_at) VALUES (?, ?)")) {
                    ps.setString(1, "payload-" + System.nanoTime());
                    ps.setLong(2, System.currentTimeMillis());
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                latencies.add(System.nanoTime() - start);
            }, executors.writer));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long globalDuration = System.currentTimeMillis() - globalStart;

        calculateAndReport("WriteOnly", operations, globalDuration, new ArrayList<>(latencies));
    }

    @Test
    @DisplayName("Benchmark: Carga Mixta (80% Lectura / 20% Escritura)")
    public void testMixedWorkload() {
        int operations = 10_000;
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Random random = new Random();

        try (Connection conn = connectionPool.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO stress_test (data) VALUES ('seed-data')");
        } catch (Exception e) {}

        long globalStart = System.currentTimeMillis();

        for (int i = 0; i < operations; i++) {
            boolean isWrite = random.nextDouble() < 0.2;

            Runnable task = () -> {
                long start = System.nanoTime();
                try (Connection conn = connectionPool.getConnection()) {
                    if (isWrite) {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO stress_test (data) VALUES (?)")) {
                            ps.setString(1, "data");
                            ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stress_test LIMIT 1")) {
                            ps.executeQuery();
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                latencies.add(System.nanoTime() - start);
            };

            if (isWrite) futures.add(CompletableFuture.runAsync(task, executors.writer));
            else futures.add(CompletableFuture.runAsync(task, executors.reader));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long globalDuration = System.currentTimeMillis() - globalStart;

        calculateAndReport("MixedLoad", operations, globalDuration, new ArrayList<>(latencies));
    }

    private void calculateAndReport(String testName, int operations, long totalDurationMs, List<Long> latencies) {
        Collections.sort(latencies);

        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));

        double throughput = (operations / (double) totalDurationMs) * 1000;

        BenchmarkReporter.log(testName, "throughput", String.format("%.2f", throughput), "ops/sec");
        BenchmarkReporter.log(testName, "p99_latency", p99 / 1000, "us");

        if (p99 > 200_000_000) {
            BenchmarkReporter.alert(testName, "Disco Lento detectado (>200ms en escritura)");
        }
    }
}
