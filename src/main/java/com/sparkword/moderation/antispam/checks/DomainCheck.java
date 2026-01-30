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

import java.util.Set;
import java.util.regex.Pattern;

public class DomainCheck implements SpamCheck {
    private static final Pattern OBFUSCATION_REMOVER = Pattern.compile("[\\[\\]\\(\\)\\{\\}]");
    private static final Pattern DOT_NORMALIZER = Pattern.compile("(\\s*\\(dot\\)\\s*|\\s*\\[dot\\]\\s*|\\s*\\(punto\\)\\s*)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> COMMON_TLDS = Set.of(
        "com", "net", "org", "edu", "gov", "mil", "int", "eu", "es", "mx", "ar", "co", "cl", "pe",
        "xyz", "info", "biz", "top", "club", "online", "pro", "site", "vip", "gg", "io", "me", "tv"
    );
    private final SparkWord plugin;

    public DomainCheck(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (!plugin.getEnvironment().getConfigManager().isDomainEnabled()) return SpamResult.PASSED;

        String raw = context.cleanMessage();
        if (raw.indexOf('.') == -1 && !containsDotKeyword(raw)) {
            return SpamResult.PASSED;
        }

        String step1 = OBFUSCATION_REMOVER.matcher(raw).replaceAll("");
        String normalized = DOT_NORMALIZER.matcher(step1).replaceAll(".");

        if (scanForDomainZeroAlloc(normalized)) {

            plugin.getEnvironment().getNotifyManager().notifyStaff(
                player,
                context.source(),
                "Anti-Domain",
                context.cleanMessage(),
                "Link/Domain"
            );
            return SpamResult.BLOCKED_WITH_REASON("spam.ip", false);
        }
        return SpamResult.PASSED;
    }

    private boolean containsDotKeyword(String text) {
        String lower = text.toLowerCase();
        return lower.contains("(dot)") || lower.contains("[dot]") || lower.contains("(punto)");
    }

    private boolean scanForDomainZeroAlloc(String text) {
        int len = text.length();
        int start = 0;

        for (int i = 0; i <= len; i++) {
            if (i == len || Character.isWhitespace(text.charAt(i))) {
                if (start < i) {
                    if (checkWordSegment(text, start, i)) {
                        return true;
                    }
                }
                start = i + 1;
            }
        }
        return false;
    }

    private boolean checkWordSegment(String text, int start, int end) {
        if ((end - start) < 4) return false;

        int lastDot = -1;
        for (int k = end - 1; k >= start; k--) {
            if (text.charAt(k) == '.') {
                lastDot = k;
                break;
            }
        }

        if (lastDot <= start || lastDot >= end - 1) return false;

        int tldStart = lastDot + 1;
        int tldEnd = end;

        while (tldEnd > tldStart) {
            char c = text.charAt(tldEnd - 1);
            if (c == ',' || c == '!' || c == '?' || c == '.') {
                tldEnd--;
            } else {
                break;
            }
        }

        if ((tldEnd - tldStart) > 6) return false;

        String tldCandidate = text.substring(tldStart, tldEnd);
        return COMMON_TLDS.contains(tldCandidate.toLowerCase());
    }
}
