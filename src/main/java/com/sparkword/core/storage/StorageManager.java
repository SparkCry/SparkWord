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
package com.sparkword.core.storage;

import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.config.FilterSettings;
import com.sparkword.core.storage.impl.StorageFactory;
import com.sparkword.core.storage.model.MuteInfo;
import com.sparkword.core.storage.spi.StorageProvider;
import com.sparkword.core.storage.spi.dao.*;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class StorageManager {

    public static final String SYSTEM_ACTOR = "CONSOLE";

    private final SparkWord plugin;
    private final StorageProvider provider;

    private PlayerDAO players;
    private MuteDAO mutes;
    private WarningDAO warnings;
    private MonitorDAO monitor;
    private AuditDAO audit;
    private SuggestionDAO suggestions;
    private ReportDAO reports;

    public StorageManager(SparkWord plugin, ConfigManager config) {
        this.plugin = plugin;

        StorageFactory factory = new StorageFactory(plugin);

        this.provider = factory.createProvider(config);

        init(config);
    }

    private void init(ConfigManager config) {
        provider.init(config);

        this.players = provider.getPlayerDAO();
        this.mutes = provider.getMuteDAO();
        this.warnings = provider.getWarningDAO();
        this.monitor = provider.getMonitorDAO();
        this.audit = provider.getAuditDAO();
        this.suggestions = provider.getSuggestionDAO();
        this.reports = provider.getReportDAO();
    }

    public void close() {
        provider.shutdown();
    }

    public void reload() {
        close();

        ConfigManager currentConfig = plugin.getEnvironment().getConfigManager();
        init(currentConfig);
    }

    public PlayerDAO getPlayers() {
        return players;
    }

    public MuteDAO getMutes() {
        return mutes;
    }

    public WarningDAO getWarnings() {
        return warnings;
    }

    public MonitorDAO getMonitor() {
        return monitor;
    }

    public AuditDAO getAudit() {
        return audit;
    }

    public SuggestionDAO getSuggestions() {
        return suggestions;
    }

    public ReportDAO getReports() {
        return reports;
    }

    public Executor getAsyncExecutor() {
        return provider.getAsyncExecutor();
    }

    public void checkAndLog(String playerName, String content, String category, String source, String detectedWord) {
        if (shouldLog(category)) {
            monitor.addLogAsync(playerName, content, category, source, detectedWord);
        }
    }

    private boolean shouldLog(String category) {
        if (category == null) return false;

        FilterSettings fs = plugin.getEnvironment().getConfigManager().getFilterSettings();

        return switch (category) {
            case "IP Split", "Anti-Domain", "Anti-Ip", "Anti-Flood", "Evasion", "Text usage (Zalgo/Unicode)",
                 "Injection" -> true;
            case "(WordBlock)", "Filter" -> !fs.isReplacementEnabled();
            default -> false;
        };
    }

    public void logAudit(String staffName, String action, String detail) {
        audit.logAuditAsync(staffName, action, detail);
    }

    public int getPlayerIdBlocking(UUID uuid, String name) {
        return players.getPlayerIdBlocking(uuid, name);
    }

    public CompletableFuture<Integer> getPlayerIdAsync(UUID uuid, String name) {
        return players.getPlayerIdAsync(uuid, name);
    }

    public CompletableFuture<Integer> getPlayerIdByNameAsync(String name) {
        return players.getPlayerIdByNameAsync(name);
    }

    public MuteInfo fetchMuteInfoBlocking(int playerId) {
        return mutes.fetchMuteInfoBlocking(playerId);
    }

    public CompletableFuture<Long> getMuteTimeAsync(int playerId) {
        return mutes.getMuteExpiryAsync(playerId);
    }

    public void mute(int playerId, String reason, String by, long durationSeconds, String actionType, MuteInfo.MuteScope scope, Consumer<Boolean> callback) {
        long now = System.currentTimeMillis();
        long expiry = durationSeconds > 0 ? now + (durationSeconds * 1000) : 0;

        mutes.muteAsync(playerId, reason, by, durationSeconds, scope)
            .whenComplete((v, ex) -> {
                if (ex != null) {
                    plugin.getLogger().warning("Error writing mute to DB for ID " + playerId + ": " + ex.getMessage());
                    if (callback != null) callback.accept(false);
                    return;
                }

                // Explicitly update cache immediately
                MuteInfo newInfo = new MuteInfo(true, by, reason, expiry, scope);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (plugin.getEnvironment().getPlayerDataManager() != null) {
                        plugin.getEnvironment().getPlayerDataManager().updateMuteDirectly(playerId, newInfo);
                    }
                });

                players.getPlayerNameAsync(playerId).thenAccept(targetName -> {
                    String detail = "Player: " + targetName + " | Reason: " + reason + " | Scope: " + scope.name();
                    audit.logAuditAsync(by, actionType, detail);

                    if (callback != null) callback.accept(true);
                }).exceptionally(e -> {
                    plugin.getLogger().warning("Error fetching player name during mute: " + e.getMessage());
                    if (callback != null) callback.accept(true);
                    return null;
                });
            });
    }

    public CompletableFuture<Void> unmute(int playerId, String by, String reason) {
        return mutes.unmuteAsync(playerId).thenCompose(v -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.getEnvironment().getPlayerDataManager() != null) {
                    plugin.getEnvironment().getPlayerDataManager().updateMuteDirectly(playerId, MuteInfo.NOT_MUTED);
                }
            });
            return players.getPlayerNameAsync(playerId);
        }).thenAccept(targetName -> {
            audit.logAuditAsync(by, "UNMUTE", "Player: " + targetName + " | Reason: " + reason);
        });
    }

    public CompletableFuture<Void> unmute(int playerId) {
        return unmute(playerId, SYSTEM_ACTOR, "Expired/Manual");
    }

    public void addWarning(int playerId, String reason, String moderator) {
        warnings.addWarningAsync(playerId, reason, moderator).thenCompose(v ->
            players.getPlayerNameAsync(playerId)
        ).thenAccept(targetName -> {
            audit.logAuditAsync(moderator, "WARN", "Player: " + targetName + " | Reason: " + reason);
        });
    }

    public CompletableFuture<Boolean> addSuggestionAsync(int playerId, String word, String reason) {
        return suggestions.addSuggestionAsync(playerId, word, reason);
    }

    public void acceptSuggestionAsync(int id, String staffName, String type, Consumer<SuggestionDAO.SuggestionInfo> callback) {
        suggestions.processSuggestionAsync(id, true).thenAccept(info -> {
            if (info != null) {
                audit.logAuditAsync(staffName, "ACCEPT", "Word: " + info.word() + " | List: " + type);
            }
            if (callback != null) callback.accept(info);
        });
    }

    public void denySuggestionAsync(int id, String staffName, Consumer<SuggestionDAO.SuggestionInfo> callback) {
        suggestions.processSuggestionAsync(id, false).thenAccept(info -> {
            if (info != null) {
                audit.logAuditAsync(staffName, "DENY", "Word: " + info.word());
            }
            if (callback != null) callback.accept(info);
        });
    }

    public CompletableFuture<List<String>> getPendingSuggestionsAsync(int page) {
        return reports.getPendingSuggestionsReportAsync(page);
    }

    public CompletableFuture<List<String>> getPlayerReportAsync(int playerId, int page) {
        return reports.getPlayerScanReportAsync(playerId, page);
    }

    public int purgeData(String type, long days) {
        if (type.equalsIgnoreCase("all")) {
            CompletableFuture<Integer> sg = suggestions.purgeAsync(days);
            CompletableFuture<Integer> bl = monitor.purgeAsync(days);
            CompletableFuture<Integer> au = audit.purgeAsync(days);
            return sg.join() + bl.join() + au.join();
        }

        CompletableFuture<Integer> future = switch (type) {
            case "sg" -> suggestions.purgeAsync(days);
            case "b" -> monitor.purgeAsync(days);
            case "a" -> audit.purgeAsync(days);
            default -> CompletableFuture.completedFuture(0);
        };
        return future.join();
    }
}
