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
package com.sparkword.moderation.filters.word;

import com.sparkword.moderation.filters.util.TextNormalizer;
import com.sparkword.moderation.filters.word.engine.AhoCorasickEngine;
import com.sparkword.moderation.filters.word.engine.AhoCorasickEngine.Match;

import java.util.ArrayList;
import java.util.List;

public class WordFilter {
    private final AhoCorasickEngine engine;
    private final WordFilterMode mode;

    public WordFilter(AhoCorasickEngine engine, WordFilterMode mode) {
        this.engine = engine;
        this.mode = mode;
    }

    public List<Match> search(String rawText, TextNormalizer.CleanMapping mapping) {

        String textToScan = mapping.cleanText();
        List<Match> rawMatches = engine.findMatches(textToScan);

        if (mode == WordFilterMode.STRONG || mode == WordFilterMode.WRITE_COMMAND) {
            return rawMatches;
        }

        List<Match> validMatches = new ArrayList<>();
        for (Match m : rawMatches) {
            if (checkBoundaries(m, mapping, rawText)) {
                validMatches.add(m);
            }
        }
        return validMatches;
    }

    private boolean checkBoundaries(Match m, TextNormalizer.CleanMapping mapping, String rawText) {
        int[] indices = mapping.originalIndices();

        if (m.start() >= indices.length || m.end() >= indices.length) return false;

        int origStart = indices[m.start()];
        int origEnd = indices[m.end()];

        if (origStart > 0) {
            char prev = rawText.charAt(origStart - 1);

            if (Character.isLetterOrDigit(prev)) return false;
        }

        if (origEnd < rawText.length() - 1) {
            char next = rawText.charAt(origEnd + 1);
            return !Character.isLetterOrDigit(next);
        }

        return true;
    }

    public WordFilterMode getMode() {
        return mode;
    }
}
