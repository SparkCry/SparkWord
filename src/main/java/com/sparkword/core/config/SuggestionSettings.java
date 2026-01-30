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
package com.sparkword.core.config;

import org.bukkit.configuration.file.FileConfiguration;

public class SuggestionSettings {
    private boolean suggestionEnabled;
    private int suggestionCooldown;
    private int suggestionMaxWord;
    private int suggestionMaxReason;

    public SuggestionSettings() {
    }

    public void load(FileConfiguration config) {
        this.suggestionEnabled = config.getBoolean("suggestion.enabled", true);
        this.suggestionCooldown = config.getInt("suggestion.suggest-cooldown", 60);
        this.suggestionMaxWord = config.getInt("suggestion.max-word-length", 20);
        this.suggestionMaxReason = config.getInt("suggestion.max-reason-length", 100);
    }

    public boolean isSuggestionEnabled() {
        return suggestionEnabled;
    }

    public int getSuggestionCooldown() {
        return suggestionCooldown;
    }

    public int getSuggestionMaxWord() {
        return suggestionMaxWord;
    }

    public int getSuggestionMaxReason() {
        return suggestionMaxReason;
    }
}
