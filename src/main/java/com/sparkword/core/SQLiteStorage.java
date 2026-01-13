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
package com.sparkword.core;

import com.sparkword.SparkWord;
import com.sparkword.model.LogEntry;
import com.sparkword.model.MuteInfo;
import com.sparkword.model.MuteInfo.MuteScope;
import com.sparkword.storage.SQLiteConnectionPool;
import com.sparkword.storage.SQLiteMigrations;
import com.sparkword.storage.executor.StorageExecutors;
import com.sparkword.storage.repositories.*;
import com.sparkword.storage.repositories.SuggestionRepository.SuggestionInfo;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SQLiteStorage {

    public static final String SYSTEM_ACTOR = "CONSOLE";

    private final SparkWord plugin;
    private SQLiteConnectionPool pool;
    private StorageExecutors executors;

    private PlayerRepository players;
    private MuteRepository mutes;
    private WarningRepository warnings;
    private MonitorRepository monitor;
    private AuditRepository audit;
    private SuggestionRepository suggestions;
    private ReportRepository reports;

    public SQLiteStorage(SparkWord plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        this.pool = new SQLiteConnectionPool(plugin);
        this.executors = new StorageExecutors();
        new SQLiteMigrations(pool, plugin.getLogger()).runMigrations();

        this.players = new PlayerRepository(pool, executors);
        this.mutes = new MuteRepository(pool, executors);
        this.warnings = new WarningRepository(pool, executors);
        this.monitor = new MonitorRepository(pool, executors);
        this.audit = new AuditRepository(pool, executors);
        this.suggestions = new SuggestionRepository(pool, executors);
        this.reports = new ReportRepository(pool, executors);
    }

    public void close() {
        if (executors != null) executors.shutdown();
        if (pool != null) pool.close();
    }

    public void reload() {
        close();
        init();
    }

    public PlayerRepository getPlayers() { return players; }
    public MuteRepository getMutes() { return mutes; }
    public WarningRepository getWarnings() { return warnings; }
    public MonitorRepository getMonitor() { return monitor; }
    public AuditRepository getAudit() { return audit; }
    public SuggestionRepository getSuggestions() { return suggestions; }
    public ReportRepository getReports() { return reports; }

    public void checkAndLog(String playerName, String content, String category, String source, String detectedWord) {
        if (shouldLog(category)) {
            monitor.addLogAsync(playerName, content, category, source, detectedWord);
        }
    }

    private boolean shouldLog(String category) {
        if (category == null) return false;
        return switch (category) {
            case "IP Split", "Anti-Domain", "Anti-Ip", "Anti-Flood", "Evasion", "Text usage (Zalgo/Unicode)", "Injection" -> true;
            case "(WordBlock)", "Filter" -> !plugin.getEnvironment().getConfigManager().isReplacementEnabled();
            default -> false;
        };
    }

    public void logAudit(String staffName, String action, String detail) {
        audit.logAuditAsync(staffName, action, detail);
    }

    public int getPlayerIdBlocking(UUID uuid, String name) { return players.getPlayerIdBlocking(uuid, name); }
    public CompletableFuture<Integer> getPlayerIdAsync(UUID uuid, String name) { return players.getPlayerIdAsync(uuid, name); }

    public MuteInfo fetchMuteInfoBlocking(int playerId) { return mutes.fetchMuteInfoBlocking(playerId); }
    public CompletableFuture<Long> getMuteTimeAsync(int playerId) { return mutes.getMuteExpiryAsync(playerId); }

    public void mute(int playerId, String reason, String by, long durationSeconds, String actionType, MuteScope scope, Consumer<Boolean> callback) {
        mutes.muteAsync(playerId, reason, by, durationSeconds, scope).thenCompose(v ->
            players.getPlayerNameAsync(playerId)
        ).thenAccept(targetName -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.getEnvironment().getPlayerDataManager() != null) {
                    plugin.getEnvironment().getPlayerDataManager().invalidateMute(playerId);
                }
            });
            String detail = "Player: " + targetName + " | Reason: " + reason + " | Scope: " + scope.name();
            audit.logAuditAsync(by, actionType, detail);
            if (callback != null) callback.accept(true);
        });
    }

    public CompletableFuture<Void> unmute(int playerId, String by, String reason) {
        return mutes.unmuteAsync(playerId).thenCompose(v ->
            players.getPlayerNameAsync(playerId)
        ).thenAccept(targetName -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.getEnvironment().getPlayerDataManager() != null) {
                    plugin.getEnvironment().getPlayerDataManager().invalidateMute(playerId);
                }
            });
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

    public void acceptSuggestionAsync(int id, String staffName, String type, Consumer<SuggestionInfo> callback) {
        suggestions.processSuggestionAsync(id, true).thenAccept(info -> {
            if (info != null) {
                audit.logAuditAsync(staffName, "ACCEPT", "Word: " + info.word() + " | List: " + type);
            }
            if (callback != null) callback.accept(info);
        });
    }

    public void denySuggestionAsync(int id, String staffName, Consumer<SuggestionInfo> callback) {
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
