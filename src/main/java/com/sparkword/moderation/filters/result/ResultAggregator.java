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
package com.sparkword.moderation.filters.result;

import com.sparkword.moderation.filters.util.TextNormalizer;
import com.sparkword.moderation.filters.word.WordFilterMode;
import com.sparkword.moderation.filters.word.engine.AhoCorasickEngine.Match;
import com.sparkword.moderation.filters.word.result.FilterResult;

import java.util.*;

public class ResultAggregator {

    private static final double EVASION_TOLERANCE = 0.05;
    private final String rawText;
    private final TextNormalizer.CleanMapping mapping;
    private final String replacementMask;
    private final boolean globalEvasionCheck;

    public ResultAggregator(String rawText, TextNormalizer.CleanMapping mapping, String replacementMask, boolean globalEvasionCheck) {
        this.rawText = rawText;
        this.mapping = mapping;
        this.replacementMask = replacementMask != null ? replacementMask : "****";
        this.globalEvasionCheck = globalEvasionCheck;
    }

    public FilterResult aggregate(Map<WordFilterMode, List<Match>> matchesByMode) {
        Set<String> detectedWords = new HashSet<>();
        String primaryReason = null;
        String primaryWord = null;

        boolean shouldBlockTotal = false;
        boolean isEvasion = false;

        if (matchesByMode.containsKey(WordFilterMode.WRITE_COMMAND) && !matchesByMode.get(WordFilterMode.WRITE_COMMAND).isEmpty()) {
            Match m = matchesByMode.get(WordFilterMode.WRITE_COMMAND).getFirst();
            return new FilterResult(true, rawText, "WriteCommand", m.word(), Set.of(m.word()), true);
        }

        List<Match> allMatches = new ArrayList<>();
        if (matchesByMode.containsKey(WordFilterMode.STRONG))
            allMatches.addAll(matchesByMode.get(WordFilterMode.STRONG));
        if (matchesByMode.containsKey(WordFilterMode.NORMAL))
            allMatches.addAll(matchesByMode.get(WordFilterMode.NORMAL));

        if (allMatches.isEmpty()) {
            return new FilterResult(false, rawText, null, null, Collections.emptySet(), false);
        }

        allMatches.sort(Comparator.comparingInt(Match::start));
        ReplacementContext replacementCtx = new ReplacementContext(rawText, replacementMask);
        int[] originalIndices = mapping.originalIndices();

        for (Match m : allMatches) {
            detectedWords.add(m.word());
            if (primaryReason == null) {
                primaryReason = "Filter (" + m.word() + ")";
                primaryWord = m.word();
            }

            if (m.start() >= originalIndices.length || m.end() >= originalIndices.length) continue;

            int origStart = originalIndices[m.start()];
            int origEnd = originalIndices[m.end()];

            int rawMatchLength = (origEnd - origStart) + 1;
            int cleanLength = m.word().length();
            int noise = rawMatchLength - cleanLength;
            double density = (double) noise / rawMatchLength;

            if (globalEvasionCheck && density > EVASION_TOLERANCE) {
                shouldBlockTotal = true;
                isEvasion = true;
                primaryReason = "Evasion";
                break;
            }

            replacementCtx.censor(origStart, origEnd);
        }

        if (shouldBlockTotal) {
            return new FilterResult(true, rawText, primaryReason, primaryWord, detectedWords, isEvasion);
        }

        return new FilterResult(false, replacementCtx.getResult(), primaryReason, primaryWord, detectedWords, false);
    }
}
