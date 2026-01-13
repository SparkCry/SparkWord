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
package com.sparkword.filters.word.loader;

import com.sparkword.SparkWord;
import com.sparkword.filters.util.TextNormalizer;
import com.sparkword.filters.word.WordFilterMode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WordListLoader {

    private final SparkWord plugin;
    private final File folder;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public WordListLoader(SparkWord plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "words");
        if (!folder.exists()) folder.mkdirs();

        for (WordFilterMode mode : WordFilterMode.values()) {
            createIfNotExists(mode.getFileName(), "# List " + mode.name());
        }
    }

    private void createIfNotExists(String name, String header) {
        File file = new File(folder, name);
        if (!file.exists()) {
            try {
                file.createNewFile();
                Files.write(file.toPath(), List.of(header), StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating " + name);
            }
        }
    }

    public Set<String> loadWords(WordFilterMode mode) {
        File file = new File(folder, mode.getFileName());
        if (!file.exists()) return Collections.emptySet();

        lock.readLock().lock();
        try (Stream<String> lines = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
            return lines
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(TextNormalizer::normalizeForSearch)
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toSet());
        } catch (IOException e) {
            plugin.getLogger().warning("Error loading list: " + mode.name());
            return Collections.emptySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    public CompletableFuture<Boolean> appendToDisk(String word, WordFilterMode mode) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, mode.getFileName());
            lock.writeLock().lock();
            try {

                Files.write(file.toPath(), List.of(word), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    public CompletableFuture<Boolean> removeWordAsync(String word, WordFilterMode mode) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, mode.getFileName());
            String target = TextNormalizer.normalizeForSearch(word);

            lock.writeLock().lock();
            try {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                boolean removed = lines.removeIf(line ->
                    TextNormalizer.normalizeForSearch(line).equals(target) || line.trim().equalsIgnoreCase(word)
                );

                if (removed) {
                    Files.write(file.toPath(), lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                    return true;
                }
                return false;
            } catch (IOException e) {
                return false;
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
}
