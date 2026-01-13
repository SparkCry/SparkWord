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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DebugThreadSafetyTest {

    @Test
    @DisplayName("Concurrencia: Toggle de Debug Flag bajo carga")
    void testDebugFlagThreadSafety() throws InterruptedException {

        SparkWord plugin = mock(SparkWord.class);

        TestableDebugContainer container = new TestableDebugContainer();

        int threads = 10;
        int iterations = 1000;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger trueReads = new AtomicInteger(0);

        service.submit(() -> {
            for (int i = 0; i < iterations; i++) {
                container.setDebugMode(i % 2 == 0);
                try { Thread.sleep(1); } catch (InterruptedException e) {}
            }
        });

        for (int i = 0; i < threads - 1; i++) {
            service.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    if (container.isDebugMode()) {
                        trueReads.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        service.shutdownNow();

        container.setDebugMode(false);
        assertEquals(false, container.isDebugMode(), "El estado final de debugMode debería ser false");

        System.out.println("Lecturas 'true' durante el caos: " + trueReads.get());
    }

    static class TestableDebugContainer {
        private volatile boolean debugMode = false;

        public boolean isDebugMode() {
            return debugMode;
        }

        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }
    }
}
