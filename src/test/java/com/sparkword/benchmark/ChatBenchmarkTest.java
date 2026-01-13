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
import com.sparkword.core.ConfigManager;
import com.sparkword.core.Environment;
import com.sparkword.filters.FilterManager;
import com.sparkword.filters.word.WordFilterMode;
import com.sparkword.filters.word.loader.WordListLoader;
import com.sparkword.util.BenchmarkReporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatBenchmarkTest {

    private static FilterManager filterManager;
    private static final int DICTIONARY_SIZE = 5000;
    private static final int MESSAGES_TO_SCAN = 10000;

    @BeforeAll
    static void setup() throws Exception {
        SparkWord plugin = mock(SparkWord.class);
        Environment env = mock(Environment.class);
        ConfigManager config = mock(ConfigManager.class);

        when(plugin.getLogger()).thenReturn(Logger.getGlobal());
        when(plugin.getEnvironment()).thenReturn(env);
        when(env.getConfigManager()).thenReturn(config);
        when(config.isUnicodeEnabled()).thenReturn(true);
        when(config.getGlobalReplacement()).thenReturn("****");

        Set<String> heavyDictionary = generateRandomWords(DICTIONARY_SIZE);

        WordListLoader loader = mock(WordListLoader.class);
        when(loader.loadWords(any(WordFilterMode.class))).thenReturn(heavyDictionary);

        long startBuild = System.nanoTime();

        filterManager = new FilterManager(plugin);
        injectLoader(filterManager, loader);

        long endBuild = System.nanoTime();
        BenchmarkReporter.log("ChatBenchmark", "trie_build_time", (endBuild - startBuild) / 1_000_000.0, "ms");
    }

    private static Set<String> generateRandomWords(int count) {
        Set<String> words = new HashSet<>();
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        while (words.size() < count) {
            sb.setLength(0);
            int len = r.nextInt(8) + 3;
            for (int i = 0; i < len; i++) {
                sb.append((char) ('a' + r.nextInt(26)));
            }
            words.add(sb.toString());
        }
        return words;
    }

    private static void injectLoader(FilterManager fm, WordListLoader loader) throws Exception {
        java.lang.reflect.Field f = FilterManager.class.getDeclaredField("loader");
        f.setAccessible(true);
        f.set(fm, loader);
        fm.loadFilters();
    }

    @Test
    @DisplayName("Stress Test: Procesamiento con Diccionario 5k")
    void testHeavyDictionaryProcessing() {
        String cleanMsg = "Hola amigo como estas";
        String dirtyMsg = "Hola " + generateRandomWords(1).iterator().next();

        long start = System.nanoTime();

        for (int i = 0; i < MESSAGES_TO_SCAN; i++) {
            filterManager.processText(i % 2 == 0 ? cleanMsg : dirtyMsg, false, null);
        }

        long duration = System.nanoTime() - start;
        double avgNs = (double) duration / MESSAGES_TO_SCAN;

        BenchmarkReporter.log("ChatBenchmark", "avg_scan_latency_5k_words", String.format("%.0f", avgNs), "ns");

        if (avgNs > 250_000) {
            BenchmarkReporter.alert("ChatBenchmark", "Latencia crítica en filtrado (>0.25ms)");
        }
    }
}
