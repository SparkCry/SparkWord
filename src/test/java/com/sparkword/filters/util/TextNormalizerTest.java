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
package com.sparkword.filters.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class TextNormalizerTest {

    private String normalize(String input) {
        return TextNormalizer.normalizeForSearch(input);
    }

    @ParameterizedTest
    @DisplayName("Debe normalizar Leetspeak y acentos agresivamente")
    @CsvSource({
        "'v4c4', 'vaca'",
        "'vácá', 'vaca'",
        "'H3LL0', 'hello'",
        "'M1erda', 'mierda'",
        "'&cvaca', 'vaca'"
    })
    void testSearchNormalization(String input, String expected) {
        String result = normalize(input);
        assertEquals(expected, result, "Falló normalización de: " + input);
    }

    @Test
    @DisplayName("Debe detectar caracteres ilegales")
    void testSecurityValidation() {
        assertFalse(TextNormalizer.validateCharacters("h\u0300o\u0301l\u0302a"), "Debió bloquear Zalgo");
        assertFalse(TextNormalizer.validateCharacters("v\u0430c\u0430"), "Debió bloquear caracteres cirílicos");
        assertTrue(TextNormalizer.validateCharacters("Hola! ¿Cómo estás?"), "Debió permitir texto válido");
    }
}
