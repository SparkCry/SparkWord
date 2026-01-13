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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventPriority;

import java.io.File;
import java.util.*;

public class ConfigManager {
    private final SparkWord plugin;

    private boolean updateCheck;
    private boolean debugMode;
    private EventPriority eventPriority;

    private boolean unicodeEnabled;
    private boolean zalgoEnabled;
    private boolean domainEnabled;
    private boolean ipEnabled;

    private boolean filterChat;
    private boolean filterSigns;
    private boolean filterBooks;
    private boolean filterAnvils;

    private boolean antiSpamEnabled;

    private boolean antiFloodEnabled;
    private int antiFloodMessages;
    private int antiFloodDelay;

    private boolean antiInjectionEnabled;
    private boolean antiInjectionGeneric;
    private boolean antiInjectionTags;
    private boolean antiInjectionPapi;

    private boolean antiRepeatEnabled;
    private int repeatSimilarity;
    private int repeatHistorySize;

    private boolean charSpamEnabled;
    private int charLimit;
    private int wordLimit;

    private boolean ipSplitEnabled;
    private int digitsLimitChat;
    private int digitsLimitWritable;

    private int bookMaxPages;
    private int bookMaxPageChars;
    private int bookOpenDelay;
    private int bookGlobalRateLimit;

    private boolean replacementEnabled;
    private String globalReplacement;

    private boolean notifyIconEnabled;
    private boolean notifyTypeFilter;
    private boolean notifyTypeFlood;
    private boolean notifyTypeIp;
    private boolean notifyTypeZalgo;
    private boolean notifyTypeInjection;

    private boolean suggestionEnabled;
    private int suggestionCooldown;
    private int suggestionMaxWord;
    private int suggestionMaxReason;

    public ConfigManager(SparkWord plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        var config = plugin.getConfig();

        this.updateCheck = config.getBoolean("update-check", true);
        this.debugMode = config.getBoolean("debug", false);
        try {
            this.eventPriority = EventPriority.valueOf(config.getString("event-priority", "HIGH").toUpperCase());
        } catch (Exception e) { this.eventPriority = EventPriority.HIGH; }

        this.unicodeEnabled = config.getBoolean("system-filter.unicode", true);
        this.zalgoEnabled = config.getBoolean("system-filter.zalgo", true);
        this.domainEnabled = config.getBoolean("system-filter.domain", true);
        this.ipEnabled = config.getBoolean("system-filter.ip", true);

        this.filterChat = config.getBoolean("filter-sources.chat", true);
        this.filterSigns = config.getBoolean("filter-sources.signs", true);
        this.filterBooks = config.getBoolean("filter-sources.books", true);
        this.filterAnvils = config.getBoolean("filter-sources.anvils", true);

        this.antiSpamEnabled = config.getBoolean("anti-spam.enabled", true);

        this.antiFloodEnabled = config.getBoolean("anti-spam.anti-flood.enabled", true);
        this.antiFloodMessages = config.getInt("anti-spam.anti-flood.messages", 4);
        this.antiFloodDelay = config.getInt("anti-spam.anti-flood.delay", 2) * 1000;

        this.antiInjectionEnabled = config.getBoolean("anti-spam.anti-injection.enabled", true);
        this.antiInjectionGeneric = config.getBoolean("anti-spam.anti-injection.generic", true);
        this.antiInjectionTags = config.getBoolean("anti-spam.anti-injection.tags", true);
        this.antiInjectionPapi = config.getBoolean("anti-spam.anti-injection.papi", true);

        this.antiRepeatEnabled = config.getBoolean("anti-spam.anti-repeat.enabled", true);
        this.repeatSimilarity = config.getInt("anti-spam.anti-repeat.similarity", 80);
        this.repeatHistorySize = config.getInt("anti-spam.anti-repeat.max-history", 2);

        this.bookMaxPages = config.getInt("anti-spam.book-limits.max-pages", 30);
        this.bookMaxPageChars = config.getInt("anti-spam.book-limits.max-chars-per-page", 256);
        this.bookOpenDelay = config.getInt("anti-spam.book-limits.open-delay", 2);
        this.bookGlobalRateLimit = config.getInt("anti-spam.book-limits.global-rate-limit", 20);

        this.suggestionEnabled = config.getBoolean("suggestion.enabled", true);
        this.suggestionCooldown = config.getInt("suggestion.suggest-cooldown", 60);
        this.suggestionMaxWord = config.getInt("suggestion.max-word-length", 20);
        this.suggestionMaxReason = config.getInt("suggestion.max-reason-length", 100);

        this.charSpamEnabled = config.getBoolean("anti-spam.character-spam.enabled", true);
        this.charLimit = config.getInt("anti-spam.character-spam.char-limit", 5);
        this.wordLimit = config.getInt("anti-spam.character-spam.word-limit", 3);

        this.ipSplitEnabled = config.getBoolean("anti-spam.ip-split-detection.enabled", true);
        this.digitsLimitChat = config.getInt("anti-spam.digits-max-limit.chat-inputs", 10);
        this.digitsLimitWritable = config.getInt("anti-spam.digits-max-limit.writable-inputs", 10);

        this.replacementEnabled = config.getBoolean("replacement.enabled", true);
        this.globalReplacement = config.getString("replacement.replace", "****");

        this.notifyIconEnabled = config.getBoolean("notifications.icon-hover.enabled", true);
        this.notifyTypeFilter = config.getBoolean("notifications.types.filter", true);
        this.notifyTypeFlood = config.getBoolean("notifications.types.flood", true);
        this.notifyTypeIp = config.getBoolean("notifications.types.ip", true);
        this.notifyTypeZalgo = config.getBoolean("notifications.types.zalgo", true);
        this.notifyTypeInjection = config.getBoolean("notifications.types.injection", true);

        plugin.setDebugMode(this.debugMode);
    }

    public boolean isUpdateCheck() { return updateCheck; }
    public boolean isDebugMode() { return debugMode; }
    public EventPriority getEventPriority() { return eventPriority; }
    public boolean isUnicodeEnabled() { return unicodeEnabled; }
    public boolean isZalgoEnabled() { return zalgoEnabled; }
    public boolean isDomainEnabled() { return domainEnabled; }
    public boolean isIpEnabled() { return ipEnabled; }
    public boolean isFilterChat() { return filterChat; }
    public boolean isFilterSigns() { return filterSigns; }
    public boolean isFilterBooks() { return filterBooks; }
    public boolean isFilterAnvils() { return filterAnvils; }

    public int getBookMaxPages() { return bookMaxPages; }
    public int getBookMaxPageChars() { return bookMaxPageChars; }
    public int getBookOpenDelay() { return bookOpenDelay; }
    public int getBookGlobalRateLimit() { return bookGlobalRateLimit; }

    public boolean isAntiSpamEnabled() { return antiSpamEnabled; }
    public boolean isAntiFloodEnabled() { return antiFloodEnabled; }
    public int getAntiFloodMessages() { return antiFloodMessages; }
    public int getAntiFloodDelay() { return antiFloodDelay; }
    public boolean isAntiInjectionEnabled() { return antiInjectionEnabled; }
    public boolean isAntiInjectionGeneric() { return antiInjectionGeneric; }
    public boolean isAntiInjectionTags() { return antiInjectionTags; }
    public boolean isAntiInjectionPapi() { return antiInjectionPapi; }

    public boolean isAntiRepeatEnabled() { return antiRepeatEnabled; }
    public int getRepeatSimilarity() { return repeatSimilarity; }
    public int getRepeatHistorySize() { return repeatHistorySize; }

    public boolean isCharSpamEnabled() { return charSpamEnabled; }
    public int getCharLimit() { return charLimit; }
    public int getWordLimit() { return wordLimit; }

    public boolean isIpSplitEnabled() { return ipSplitEnabled; }
    public int getDigitsLimitChat() { return digitsLimitChat; }
    public int getDigitsLimitWritable() { return digitsLimitWritable; }

    public boolean isReplacementEnabled() { return replacementEnabled; }
    public String getGlobalReplacement() { return globalReplacement; }

    public boolean isNotifyIconEnabled() { return notifyIconEnabled; }
    public boolean isNotifyTypeFilter() { return notifyTypeFilter; }
    public boolean isNotifyTypeFlood() { return notifyTypeFlood; }
    public boolean isNotifyTypeIp() { return notifyTypeIp; }
    public boolean isNotifyTypeZalgo() { return notifyTypeZalgo; }
    public boolean isNotifyTypeInjection() { return notifyTypeInjection; }

    public boolean isSuggestionEnabled() { return suggestionEnabled; }
    public int getSuggestionCooldown() { return suggestionCooldown; }
    public int getSuggestionMaxWord() { return suggestionMaxWord; }
    public int getSuggestionMaxReason() { return suggestionMaxReason; }

}
