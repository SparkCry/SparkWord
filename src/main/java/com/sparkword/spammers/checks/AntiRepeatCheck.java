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
package com.sparkword.spammers.checks;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sparkword.SparkWord;
import com.sparkword.spammers.SpamCheck;
import com.sparkword.spammers.SpamContext;
import com.sparkword.spammers.SpamManager.SpamResult;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.LinkedList;
import java.util.UUID;

public class AntiRepeatCheck implements SpamCheck {

    private final SparkWord plugin;
    private final Cache<UUID, LinkedList<String>> historyCache;

    public AntiRepeatCheck(SparkWord plugin) {
        this.plugin = plugin;
        this.historyCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(1000)
            .executor(Runnable::run)
            .build();
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (!context.isChat() || !context.checkTraffic()) return SpamResult.PASSED;
        if (!plugin.getEnvironment().getConfigManager().isAntiRepeatEnabled()) return SpamResult.PASSED;

        String cleanMsg = context.cleanMessage();
        if (cleanMsg.isEmpty()) return SpamResult.PASSED;

        if (cleanMsg.length() > 5 && checkInternalPeriodicityKMP(cleanMsg)) {
            return SpamResult.BLOCKED_WITH_REASON("spam.repeat", false);
        }

        UUID uuid = player.getUniqueId();
        LinkedList<String> history = historyCache.get(uuid, k -> new LinkedList<>());

        String current = cleanMsg.toLowerCase().trim();
        int similarityThreshold = plugin.getEnvironment().getConfigManager().getRepeatSimilarity();

        boolean blocked = false;

        synchronized (history) {
            for (String prev : history) {
                if (current.length() < 5 || prev.length() < 5) {
                    if (current.equalsIgnoreCase(prev)) {
                        blocked = true;
                        break;
                    }
                } else {
                    if (calculateSimilarity(current, prev) >= similarityThreshold) {
                        blocked = true;
                        break;
                    }
                }
            }

            if (blocked) {
                return SpamResult.BLOCKED_WITH_REASON("spam.repeat", false);
            }

            history.addFirst(current);
            if (history.size() > plugin.getEnvironment().getConfigManager().getRepeatHistorySize()) {
                history.removeLast();
            }
        }

        return SpamResult.PASSED;
    }

    private boolean checkInternalPeriodicityKMP(String text) {
        int n = text.length();
        int[] pi = new int[n];
        for (int i = 1, j = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != text.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == text.charAt(j)) j++;
            pi[i] = j;
        }
        int len = pi[n - 1];
        int k = n - len;
        return len > 0 && n % k == 0 && (n / k) >= 3;
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 100.0;

        if (Math.abs(s1.length() - s2.length()) > Math.max(s1.length(), s2.length()) * 0.5) return 0.0;

        int matches = 0;
        int len = Math.min(s1.length(), s2.length());
        for(int i=0; i<len; i++) {
            if (s1.charAt(i) == s2.charAt(i)) matches++;
        }
        return (double) matches / Math.max(s1.length(), s2.length()) * 100.0;
    }
}
