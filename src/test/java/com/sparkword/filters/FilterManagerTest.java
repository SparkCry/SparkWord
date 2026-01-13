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

import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.Environment;
import com.sparkword.filters.word.WordFilter;
import com.sparkword.filters.word.WordFilterMode;
import com.sparkword.filters.word.engine.AhoCorasickEngine;
import com.sparkword.filters.word.loader.WordListLoader;
import com.sparkword.filters.word.result.FilterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FilterManagerTest {

    private FilterManager filterManager;

    @Mock private SparkWord plugin;
    @Mock private Environment environment;
    @Mock private ConfigManager configManager;
    @Mock private WordListLoader wordListLoader;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(plugin.getEnvironment()).thenReturn(environment);
        when(plugin.getLogger()).thenReturn(Logger.getGlobal());
        when(environment.getConfigManager()).thenReturn(configManager);

        when(configManager.isUnicodeEnabled()).thenReturn(true);
        when(configManager.isReplacementEnabled()).thenReturn(false);

        filterManager = new FilterManager(plugin, wordListLoader);

        Map<WordFilterMode, WordFilter> activeFilters = getPrivateMap(filterManager, "activeFilters");

        activeFilters.put(WordFilterMode.NORMAL,
                new WordFilter(AhoCorasickEngine.fromWords(Set.of("vaca")), WordFilterMode.NORMAL));

        activeFilters.put(WordFilterMode.STRONG,
                new WordFilter(AhoCorasickEngine.fromWords(Set.of("gato")), WordFilterMode.STRONG));

        activeFilters.put(WordFilterMode.WRITE_COMMAND,
                new WordFilter(AhoCorasickEngine.fromWords(Set.of("mod")), WordFilterMode.WRITE_COMMAND));
    }

    @SuppressWarnings("unchecked")
    private Map<WordFilterMode, WordFilter> getPrivateMap(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<WordFilterMode, WordFilter>) field.get(target);
    }

    @DisplayName("Filtro NORMAL: Palabra 'vaca' y evasiones")
    @ParameterizedTest(name = "Input: {0}")
    @ValueSource(strings = {
            "vaca", "Vaca", "vAca", "VACA",
            "v a c a", "v.a.c.a", "v/a/c/a",
            "Va/ca", "V/aca", "Vac/a",
            "v-a-c-a", "v⚡a⚡c⚡a"
    })
    void testNormalVaca(String input) {
        FilterResult result = filterManager.processText(input, false, null);
        assertTrue(result.blocked() || !result.detectedWords().isEmpty(),
                "No detectó 'vaca' → input=" + input + " result=" + result);
    }

    @DisplayName("Filtro STRONG: Palabra 'gato' y evasiones extremas")
    @ParameterizedTest(name = "Input: {0}")
    @ValueSource(strings = {
            "gato", "Gato", "G.ato", "Gat.o",
            "g a t o", "g.a.t.o", "g/a/t/o",
            "g-a-t-o", "g⚡a⚡t⚡o", "GATO", "GaTo"
    })
    void testStrongGato(String input) {
        FilterResult result = filterManager.processText(input, false, null);
        assertTrue(result.blocked() || !result.detectedWords().isEmpty(),
                "No detectó 'gato' → input=" + input + " result=" + result);
    }

    @DisplayName("Filtro WRITE_COMMAND: Palabra 'mod'")
    @ParameterizedTest(name = "Input: {0}")
    @ValueSource(strings = {
            "mod", "Mod", "MOD", "m.o.d"
    })
    void testCommandMod(String input) {
        FilterResult result = filterManager.processWriteCommand(input);
        assertTrue(result.blocked(),
                "No bloqueó comando 'mod' → input=" + input);
    }
}
