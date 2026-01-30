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
package com.sparkword.filters;

import com.sparkword.Environment;
import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.config.FilterSettings;
import com.sparkword.moderation.filters.FilterManager;
import com.sparkword.moderation.filters.word.WordFilterMode;
import com.sparkword.moderation.filters.word.loader.WordListLoader;
import com.sparkword.moderation.filters.word.result.FilterResult;
import com.sparkword.util.BenchmarkReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FilterManagerConcurrencyTest {

    @Mock
    private SparkWord plugin;
    @Mock
    private Environment environment;
    @Mock
    private ConfigManager configManager;
    @Mock
    private FilterSettings filterSettings;
    @Mock
    private WordListLoader wordListLoader;
    private FilterManager filterManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getEnvironment()).thenReturn(environment);
        when(plugin.getLogger()).thenReturn(Logger.getGlobal());
        when(environment.getConfigManager()).thenReturn(configManager);

        when(configManager.getFilterSettings()).thenReturn(filterSettings);
        when(filterSettings.isUnicodeEnabled()).thenReturn(false);
        when(filterSettings.getGlobalReplacement()).thenReturn("****");

        filterManager = new FilterManager(plugin, wordListLoader);
    }

    @Test
    @DisplayName("Carga lenta NO debe bloquear processText")
    void testSlowLoadingDoesNotBlockChat() throws InterruptedException {
        CountDownLatch loadStartedLatch = new CountDownLatch(1);
        when(wordListLoader.loadWords(any(WordFilterMode.class))).thenAnswer(invocation -> {
            loadStartedLatch.countDown();
            Thread.sleep(1000);
            return Collections.emptySet();
        });

        long startLoadTime = System.nanoTime();
        CompletableFuture<Void> loadFuture = filterManager.loadFilters();
        loadStartedLatch.await(2, TimeUnit.SECONDS);

        long startProcessTime = System.nanoTime();
        FilterResult result = filterManager.processText("Mensaje de prueba", false, null);
        long endProcessTime = System.nanoTime();

        long processDurationMs = TimeUnit.NANOSECONDS.toMillis(endProcessTime - startProcessTime);

        BenchmarkReporter.log("ConcurrencyTest", "chat_latency_during_load", processDurationMs, "ms");

        assertTrue(processDurationMs < 100, "El hilo principal se bloqueó.");
        assertDoesNotThrow(() -> loadFuture.join());
    }
}
