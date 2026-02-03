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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sparkword.SparkWord;
import com.sparkword.moderation.antispam.SpamCheck;
import com.sparkword.moderation.antispam.SpamContext;
import com.sparkword.moderation.antispam.SpamManager.SpamResult;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

public class AntiRepeatCheck implements SpamCheck {

    private static final int HISTORY_BUFFER_SIZE = 5;
    private final SparkWord plugin;
    private final Cache<UUID, UserHistory> historyCache;

    public AntiRepeatCheck(SparkWord plugin) {
        this.plugin = plugin;
        this.historyCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(2000)
            .build();
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (!context.isChat() || !context.checkTraffic()) return SpamResult.PASSED;
        if (!plugin.getEnvironment().getConfigManager().isAntiRepeatEnabled()) return SpamResult.PASSED;
        if (player.hasPermission("sparkword.bypass.repeat")) return SpamResult.PASSED;

        String cleanMsg = context.cleanMessage().trim();
        if (cleanMsg.length() < 2) return SpamResult.PASSED;

        UUID uuid = player.getUniqueId();
        UserHistory history = historyCache.get(uuid, k -> new UserHistory());

        long now = System.currentTimeMillis();
        int similarityThreshold = plugin.getEnvironment().getConfigManager().getRepeatSimilarity();
        int maxRepeats = Math.max(1, plugin.getEnvironment().getConfigManager().getRepeatHistorySize());
        long cooldownSec = Math.max(1, plugin.getEnvironment().getConfigManager().getRepeatCooldown());

        synchronized (history) {
            Iterator<MessageStat> it = history.messages.iterator();
            MessageStat match = null;
            double matchScore = 0.0;

            while (it.hasNext()) {
                MessageStat stat = it.next();

                if (stat.content.equalsIgnoreCase(cleanMsg)) {
                    match = stat;
                    matchScore = 100.0;
                    it.remove();
                    break;
                }

                double similarity = calculateSimilarity(cleanMsg, stat.content);
                if (similarity >= similarityThreshold) {
                    match = stat;
                    matchScore = similarity;
                    it.remove();
                    break;
                }
            }

            if (plugin.isDebugMode() && match != null) {
                plugin.getLogger().info("[AntiRepeat] Matched '" + cleanMsg + "' with '" + match.content + "' (Score: " + String.format("%.2f", matchScore) + ")");
            }

            if (match != null) {
                if (match.blockedUntil > now) {
                    history.messages.addFirst(match);
                    return SpamResult.BLOCKED_WITH_REASON("spam.repeat", false);
                }

                long timeSinceLast = now - match.lastSeen;
                boolean shouldReset = (match.blockedUntil != 0 && match.blockedUntil <= now)
                    || (timeSinceLast > (cooldownSec * 1000L + 30000L));

                if (shouldReset) {
                    if (plugin.isDebugMode())
                        plugin.getLogger().info("[AntiRepeat] Resetting count for '" + match.content + "' (Expired/Decayed)");
                    match.count = 0;
                    match.blockedUntil = 0;
                }

                match.count++;
                match.content = cleanMsg;
                match.lastSeen = now;
                history.messages.addFirst(match);

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[AntiRepeat] Count: " + match.count + " / Limit: " + maxRepeats);
                }

                if (match.count > maxRepeats) {
                    match.blockedUntil = now + (cooldownSec * 1000L);
                    match.count = 0;

                    if (plugin.isDebugMode())
                        plugin.getLogger().info("[AntiRepeat] Blocking '" + match.content + "' until " + match.blockedUntil);

                    return SpamResult.BLOCKED_WITH_REASON("spam.repeat", false);
                }

            } else {
                if (plugin.isDebugMode()) plugin.getLogger().info("[AntiRepeat] New message: " + cleanMsg);

                MessageStat newStat = new MessageStat(cleanMsg, now);
                history.messages.addFirst(newStat);

                if (history.messages.size() > HISTORY_BUFFER_SIZE) {
                    history.messages.removeLast();
                }
            }
        }

        return SpamResult.PASSED;
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equalsIgnoreCase(s2)) return 100.0;

        String l1 = s1.toLowerCase();
        String l2 = s2.toLowerCase();

        int longer = Math.max(l1.length(), l2.length());
        if (longer == 0) return 100.0;

        if (Math.abs(l1.length() - l2.length()) > longer * 0.4) return 0.0;

        int dist = getLevenshteinDistance(l1, l2);
        return (longer - dist) / (double) longer * 100.0;
    }

    private int getLevenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];
        for (int i = 0; i <= x.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= y.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= x.length(); i++) {
            for (int j = 1; j <= y.length(); j++) {
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1));
            }
        }
        return dp[x.length()][y.length()];
    }

    private static class UserHistory {
        final LinkedList<MessageStat> messages = new LinkedList<>();
    }

    private static class MessageStat {
        String content;
        int count;
        long blockedUntil;
        long lastSeen;

        MessageStat(String content, long now) {
            this.content = content;
            this.count = 1;
            this.blockedUntil = 0;
            this.lastSeen = now;
        }
    }
}
