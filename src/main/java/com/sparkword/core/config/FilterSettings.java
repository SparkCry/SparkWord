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

public class FilterSettings {
    private boolean unicodeEnabled;
    private boolean zalgoEnabled;
    private boolean filterChat;
    private boolean filterSigns;
    private boolean filterBooks;
    private boolean filterAnvils;
    private boolean replacementEnabled;
    private String globalReplacement;

    public FilterSettings() {
    }

    public void load(FileConfiguration config) {
        this.unicodeEnabled = config.getBoolean("system-filter.unicode", true);
        this.zalgoEnabled = config.getBoolean("system-filter.zalgo", true);
        this.filterChat = config.getBoolean("filter-sources.chat", true);
        this.filterSigns = config.getBoolean("filter-sources.signs", true);
        this.filterBooks = config.getBoolean("filter-sources.books", true);
        this.filterAnvils = config.getBoolean("filter-sources.anvils", true);

        this.replacementEnabled = config.getBoolean("replacement.enabled", true);
        this.globalReplacement = config.getString("replacement.replace", "****");
    }

    public boolean isUnicodeEnabled() {
        return unicodeEnabled;
    }

    public boolean isZalgoEnabled() {
        return zalgoEnabled;
    }

    public boolean isFilterChat() {
        return filterChat;
    }

    public boolean isFilterSigns() {
        return filterSigns;
    }

    public boolean isFilterBooks() {
        return filterBooks;
    }

    public boolean isFilterAnvils() {
        return filterAnvils;
    }

    public boolean isReplacementEnabled() {
        return replacementEnabled;
    }

    public String getGlobalReplacement() {
        return globalReplacement;
    }
}
