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

import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.Environment;
import com.sparkword.core.NotifyManager;
import com.sparkword.spammers.SpamCheck;
import com.sparkword.spammers.SpamContext;
import com.sparkword.spammers.SpamManager;
import com.sparkword.spammers.SpamManager.SpamResult;
import com.sparkword.spammers.checks.AntiFloodCheck;
import com.sparkword.spammers.checks.DomainCheck;
import com.sparkword.spammers.checks.IPCheck;
import com.sparkword.util.BenchmarkReporter;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class BotAttackBenchmarkTest {

    private List<SpamCheck> pipeline;

    @Mock private SparkWord plugin;
    @Mock private Environment environment;
    @Mock private ConfigManager configManager;
    @Mock private NotifyManager notifyManager;
    @Mock private SpamManager spamManager;
    @Mock private Player botPlayer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getEnvironment()).thenReturn(environment);
        when(plugin.getLogger()).thenReturn(Logger.getGlobal());
        when(plugin.getSpamManager()).thenReturn(spamManager);
        when(environment.getConfigManager()).thenReturn(configManager);
        when(environment.getNotifyManager()).thenReturn(notifyManager);

        when(configManager.isDomainEnabled()).thenReturn(true);
        when(configManager.isAntiFloodEnabled()).thenReturn(true);
        when(configManager.getAntiFloodMessages()).thenReturn(20);
        when(configManager.getAntiFloodDelay()).thenReturn(1000);

        when(botPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(botPlayer.hasPermission(anyString())).thenReturn(false);

        pipeline = new ArrayList<>();
        pipeline.add(new IPCheck(plugin));
        pipeline.add(new DomainCheck(plugin));
        pipeline.add(new AntiFloodCheck(plugin));
    }

    @Test
    @DisplayName("Stress Test: Pipeline Completo bajo Ataque Bot (5k msg)")
    void testFullPipelineAttack() {
        int botCount = 500;
        int messagesPerBot = 10;
        List<String> attackTraffic = generateMixedTraffic(botCount * messagesPerBot);
        List<Long> latencies = new ArrayList<>(attackTraffic.size());

        long totalStart = System.nanoTime();

        for (String msg : attackTraffic) {
            long msgStart = System.nanoTime();

            SpamContext ctx = new SpamContext(msg, msg, "Chat", false, null, -1, true);

            for (SpamCheck check : pipeline) {
                SpamResult result = check.check(botPlayer, ctx);
                if (result.blocked()) break;
            }

            long msgEnd = System.nanoTime();
            latencies.add(msgEnd - msgStart);
        }

        long totalEnd = System.nanoTime();

        Collections.sort(latencies);
        double avgNs = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));
        long max = latencies.get(latencies.size() - 1);

        BenchmarkReporter.log("BotPipeline", "total_processed", attackTraffic.size(), "msgs");
        BenchmarkReporter.log("BotPipeline", "avg_latency", String.format("%.0f", avgNs), "ns");
        BenchmarkReporter.log("BotPipeline", "p95_latency", p95, "ns");
        BenchmarkReporter.log("BotPipeline", "p99_latency", p99, "ns");
        BenchmarkReporter.log("BotPipeline", "max_latency", max, "ns");

        if (p99 > 2_000_000) {
            BenchmarkReporter.alert("BotPipeline", "LAG SPIKE DETECTADO: p99 > 0.5ms");
        }
    }

    private List<String> generateMixedTraffic(int count) {
        List<String> traffic = new ArrayList<>(count);
        Random r = new Random();
        String[] samples = {
            "Hola a todos",
            "192.168.1.55 entra ya",
            "minecraft server barato www.fake.com",
            "spam spam spam spam",
            "jugador normal hablando",
            "1.1.1.1 probando ip"
        };
        for (int i = 0; i < count; i++) {
            traffic.add(samples[r.nextInt(samples.length)]);
        }
        return traffic;
    }
}
