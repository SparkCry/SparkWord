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

import com.sparkword.Environment;
import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.NotifyManager;
import com.sparkword.core.config.AntiSpamSettings;
import com.sparkword.moderation.antispam.SpamContext;
import com.sparkword.moderation.antispam.SpamManager;
import com.sparkword.moderation.antispam.checks.AntiFloodCheck;
import com.sparkword.util.BenchmarkReporter;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AntiFloodStressTest {

    private AntiFloodCheck antiFloodCheck;

    @Mock
    private SparkWord plugin;
    @Mock
    private Environment environment;
    @Mock
    private ConfigManager configManager;
    @Mock
    private AntiSpamSettings antiSpamSettings;
    @Mock
    private NotifyManager notifyManager;
    @Mock
    private SpamManager spamManager;
    @Mock
    private Player player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(plugin.getEnvironment()).thenReturn(environment);
        when(plugin.getLogger()).thenReturn(Logger.getGlobal());
        when(plugin.getSpamManager()).thenReturn(spamManager);
        when(environment.getConfigManager()).thenReturn(configManager);
        when(environment.getNotifyManager()).thenReturn(notifyManager);

        when(configManager.getAntiSpamSettings()).thenReturn(antiSpamSettings);
        when(configManager.isAntiFloodEnabled()).thenReturn(true);
        when(configManager.getAntiFloodMessages()).thenReturn(5);
        when(configManager.getAntiFloodDelay()).thenReturn(100);

        UUID fixedUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(player.getUniqueId()).thenReturn(fixedUUID);
        when(player.getName()).thenReturn("SpamBot");
        when(player.hasPermission(anyString())).thenReturn(false);

        antiFloodCheck = new AntiFloodCheck(plugin);
    }

    @Test
    @DisplayName("Anti-Flood Efficiency: 10,000 Burst Messages")
    void testAntiFloodPerformance() {
        int totalMessages = 10000;
        SpamContext context = new SpamContext("Spam message", "spam message", "Chat", false, null, -1, true);

        for (int i = 0; i < 1000; i++) {
            antiFloodCheck.check(player, context);
        }

        antiFloodCheck = new AntiFloodCheck(plugin);

        long startTime = System.nanoTime();
        int blockedCount = 0;

        for (int i = 0; i < totalMessages; i++) {
            SpamManager.SpamResult result = antiFloodCheck.check(player, context);
            if (result.blocked()) blockedCount++;
        }

        long endTime = System.nanoTime();
        double avgNs = (double) (endTime - startTime) / totalMessages;

        BenchmarkReporter.log("AntiFlood", "avg_processing_time", String.format("%.2f", avgNs), "ns");
        BenchmarkReporter.log("AntiFlood", "blocked_messages", blockedCount, "msgs");

        assertTrue(avgNs < 150000, "Check too slow.");
        assertTrue(blockedCount > 9900, "Anti-Flood did not block enough. Blocked: " + blockedCount);
    }
}
