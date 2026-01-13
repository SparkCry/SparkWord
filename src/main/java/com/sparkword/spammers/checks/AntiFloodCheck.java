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
import com.sparkword.spammers.SpamManager.PunishmentType;
import com.sparkword.spammers.SpamManager.SpamResult;
import com.sparkword.spammers.antiflood.AtomicTokenBucket;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AntiFloodCheck implements SpamCheck {

    private final SparkWord plugin;
    private final Cache<UUID, AtomicTokenBucket> floodBuckets;
    private final Cache<UUID, ConcurrentLinkedDeque<String>> messageHistory;

    private volatile long cachedCapacity;
    private volatile double cachedRefillRate;
    private volatile boolean cachedEnabled;

    public AntiFloodCheck(SparkWord plugin) {
        this.plugin = plugin;

        this.messageHistory = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(15))
            .executor(Runnable::run)
            .build();

        this.floodBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(2000)
            .executor(Runnable::run)
            .build();

        reload();
    }

    public void reload() {
        this.cachedEnabled = plugin.getEnvironment().getConfigManager().isAntiFloodEnabled();

        long cap = plugin.getEnvironment().getConfigManager().getAntiFloodMessages();
        int delayMs = plugin.getEnvironment().getConfigManager().getAntiFloodDelay();

        this.cachedCapacity = cap;
        this.cachedRefillRate = (double) cap / (Math.max(1, delayMs) / 1000.0);

        floodBuckets.invalidateAll();
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (!cachedEnabled) return SpamResult.PASSED;
        if (!context.checkTraffic() || context.isWritable()) return SpamResult.PASSED;

        UUID uuid = player.getUniqueId();

        ConcurrentLinkedDeque<String> history = messageHistory.get(uuid, k -> new ConcurrentLinkedDeque<>());
        history.add(context.message());

        if (history.size() > 5) {
            history.pollFirst();
        }

        AtomicTokenBucket bucket = floodBuckets.get(uuid, k ->
            new AtomicTokenBucket(cachedCapacity, cachedRefillRate)
        );

        if (!bucket.tryConsume()) {
            plugin.getSpamManager().triggerAutoMute(
                player,
                "anti-spam.anti-flood.mute", 
                "15m",
                "Anti-Flood Auto Mute",
                PunishmentType.MUTE
            );

            String floodContext = String.join(" | ", history);
            history.clear();

            plugin.getEnvironment().getNotifyManager().notifyStaff(player, "Chat", "Anti-Flood", floodContext, "Rate Limit");

            return SpamResult.BLOCKED_WITH_REASON("spam.flood", false);
        }

        return SpamResult.PASSED;
    }
}
