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
package com.sparkword.moderation.antispam.checks;

import com.sparkword.SparkWord;
import com.sparkword.moderation.antispam.SpamCheck;
import com.sparkword.moderation.antispam.SpamContext;
import com.sparkword.moderation.antispam.SpamManager.SpamResult;
import org.bukkit.entity.Player;

public class CharSpamCheck implements SpamCheck {

    private final SparkWord plugin;
    private int charLimit;
    private int wordLimit;

    public CharSpamCheck(SparkWord plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.charLimit = Math.max(3, plugin.getEnvironment().getConfigManager().getCharLimit());
        this.wordLimit = Math.max(2, plugin.getEnvironment().getConfigManager().getWordLimit());
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (!plugin.getEnvironment().getConfigManager().isCharSpamEnabled()) return SpamResult.PASSED;

        String msg = context.cleanMessage();
        if (msg.length() < 3) return SpamResult.PASSED;

        if (checkCharRepetition(msg)) {
            return SpamResult.BLOCKED_WITH_REASON("spam.chars", false);
        }

        if (checkWordRepetition(msg)) {
            return SpamResult.BLOCKED_WITH_REASON("spam.repeat", false);
        }

        return SpamResult.PASSED;
    }

    private boolean checkCharRepetition(String text) {
        int count = 1;
        char last = text.charAt(0);
        int len = text.length();

        for (int i = 1; i < len; i++) {
            char current = text.charAt(i);
            if (current == last) {
                count++;
                if (count > charLimit) return true;
            } else {
                count = 1;
                last = current;
            }
        }
        return false;
    }

    private boolean checkWordRepetition(String text) {
        int len = text.length();
        int repetitions = 1;

        int currentWordStart = 0;
        int currentWordEnd = findNextSeparator(text, 0);

        String lastWord = text.substring(currentWordStart, currentWordEnd);

        int scanIndex = currentWordEnd;

        while (scanIndex < len) {
            while (scanIndex < len && text.charAt(scanIndex) == ' ') scanIndex++;
            if (scanIndex >= len) break;

            int nextWordEnd = findNextSeparator(text, scanIndex);
            int wordLen = nextWordEnd - scanIndex;

            if (wordLen == lastWord.length() && text.regionMatches(true, scanIndex, lastWord, 0, wordLen)) {
                repetitions++;
                if (repetitions > wordLimit) return true;
            } else {
                lastWord = text.substring(scanIndex, nextWordEnd);
                repetitions = 1;
            }

            scanIndex = nextWordEnd;
        }

        return false;
    }

    private int findNextSeparator(String text, int start) {
        int i = text.indexOf(' ', start);
        return i == -1 ? text.length() : i;
    }
}
