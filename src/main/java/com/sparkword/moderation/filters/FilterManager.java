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
package com.sparkword.moderation.filters;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sparkword.SparkWord;
import com.sparkword.moderation.filters.result.ResultAggregator;
import com.sparkword.moderation.filters.util.TextNormalizer;
import com.sparkword.moderation.filters.word.WordFilter;
import com.sparkword.moderation.filters.word.WordFilterMode;
import com.sparkword.moderation.filters.word.engine.AhoCorasickEngine;
import com.sparkword.moderation.filters.word.engine.AhoCorasickEngine.Match;
import com.sparkword.moderation.filters.word.loader.WordListLoader;
import com.sparkword.moderation.filters.word.result.FilterResult;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

public class FilterManager {

    private final SparkWord plugin;
    private final WordListLoader loader;

    private final Map<WordFilterMode, WordFilter> activeFilters = new ConcurrentHashMap<>();
    private final Map<WordFilterMode, Set<String>> rawListCache = new ConcurrentHashMap<>();
    private final Cache<String, Pattern> patternCache;

    private final AtomicBoolean isReloading = new AtomicBoolean(false);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FilterManager(SparkWord plugin) {
        this(plugin, new WordListLoader(plugin));
    }

    public FilterManager(SparkWord plugin, WordListLoader loader) {
        this.plugin = plugin;
        this.loader = loader;

        this.patternCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(5000)
            .build();
    }

    public CompletableFuture<Void> loadFilters() {
        if (!isReloading.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                Map<WordFilterMode, Set<String>> tempRawList = new EnumMap<>(WordFilterMode.class);
                Map<WordFilterMode, WordFilter> tempFilters = new EnumMap<>(WordFilterMode.class);

                for (WordFilterMode mode : WordFilterMode.values()) {
                    Set<String> words = loader.loadWords(mode);
                    tempRawList.put(mode, ConcurrentHashMap.newKeySet(words.size()));
                    tempRawList.get(mode).addAll(words);

                    AhoCorasickEngine engine = AhoCorasickEngine.fromWords(words);
                    tempFilters.put(mode, new WordFilter(engine, mode));
                }

                lock.writeLock().lock();
                try {
                    patternCache.invalidateAll();
                    rawListCache.putAll(tempRawList);
                    activeFilters.putAll(tempFilters);
                } finally {
                    lock.writeLock().unlock();
                }

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Filters reloaded successfully.");
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Error loading filters: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isReloading.set(false);
            }
        });
    }

    public CompletableFuture<Boolean> addWordHotSwap(String word, WordFilterMode mode) {
        return CompletableFuture.supplyAsync(() -> {
            String normalized = TextNormalizer.normalizeForSearch(word);

            lock.readLock().lock();
            try {
                Set<String> currentList = rawListCache.get(mode);
                if (currentList != null && currentList.contains(normalized)) {
                    return false;
                }
            } finally {
                lock.readLock().unlock();
            }

            lock.writeLock().lock();
            try {
                Set<String> currentList = rawListCache.computeIfAbsent(mode, k -> ConcurrentHashMap.newKeySet());
                if (currentList.contains(normalized)) return false;

                currentList.add(normalized);
                AhoCorasickEngine newEngine = AhoCorasickEngine.fromWords(currentList);
                activeFilters.put(mode, new WordFilter(newEngine, mode));
                return true;
            } finally {
                lock.writeLock().unlock();
            }
        }).thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            return loader.appendToDisk(word, mode);
        });
    }

    public Pattern getCachedPattern(String word) {
        return patternCache.get(word, w ->
            Pattern.compile(Pattern.quote(w), Pattern.CASE_INSENSITIVE));
    }

    public Set<String> getList(WordFilterMode mode) {
        return rawListCache.getOrDefault(mode, Collections.emptySet());
    }

    public WordListLoader getLoader() {
        return loader;
    }

    public FilterResult processText(String rawText, boolean isWritable, Player player) {
        return processInternal(rawText, isWritable, false, player);
    }

    public FilterResult processWriteCommand(String rawText) {
        return processInternal(rawText, true, true, null);
    }

    private FilterResult processInternal(String rawText, boolean isWritable, boolean checkWC, Player player) {
        if (plugin.getEnvironment().getConfigManager().isUnicodeEnabled()) {
            if (!TextNormalizer.validateCharacters(rawText)) {
                if (player == null || !player.hasPermission("sparkword.bypass.symbol.all")) {

                    String safeLog = rawText.length() > 50 ? rawText.substring(0, 50) + "..." : rawText;

                    return new FilterResult(true, safeLog, "Zalgo/Unicode", "Unicode", Set.of(), true);
                }
            }
        }

        TextNormalizer.CleanMapping mapping = TextNormalizer.buildCleanMapping(rawText);
        Map<WordFilterMode, List<Match>> matches = new EnumMap<>(WordFilterMode.class);

        lock.readLock().lock();
        try {
            if (checkWC) {
                var filter = activeFilters.get(WordFilterMode.WRITE_COMMAND);
                if (filter != null) matches.put(WordFilterMode.WRITE_COMMAND, filter.search(rawText, mapping));
            }

            var strong = activeFilters.get(WordFilterMode.STRONG);
            if (strong != null) matches.put(WordFilterMode.STRONG, strong.search(rawText, mapping));

            var normal = activeFilters.get(WordFilterMode.NORMAL);
            if (normal != null) matches.put(WordFilterMode.NORMAL, normal.search(rawText, mapping));

        } finally {
            lock.readLock().unlock();
        }

        String replacement = plugin.getEnvironment().getConfigManager().getGlobalReplacement();
        boolean checkEvasion = plugin.getEnvironment().getConfigManager().isUnicodeEnabled();

        ResultAggregator aggregator = new ResultAggregator(rawText, mapping, replacement, checkEvasion);
        return aggregator.aggregate(matches);
    }
}
