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

import com.sparkword.util.TimeUtil;
import org.bukkit.configuration.file.FileConfiguration;

public class AntiSpamSettings {
    private boolean domainEnabled;
    private boolean ipEnabled;
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
    private long repeatCooldown;
    private boolean charSpamEnabled;
    private int charLimit;
    private int wordLimit;
    private boolean ipSplitEnabled;
    private int digitsLimitChat;
    private int digitsLimitWritable;

    private boolean capsEnabled;
    private int capsLimit;

    private int bookMaxPages;
    private int bookMaxPageChars;
    private int bookOpenDelay;
    private int bookGlobalRateLimit;

    public AntiSpamSettings() {
    }

    public void load(FileConfiguration config) {
        this.domainEnabled = config.getBoolean("system-filter.domain", true);
        this.ipEnabled = config.getBoolean("system-filter.ip", true);

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
        this.repeatHistorySize = config.getInt("anti-spam.anti-repeat.max-history", 3);

        String repeatTimeStr = config.getString("anti-spam.anti-repeat.cooldown", "10s");
        this.repeatCooldown = TimeUtil.parseDuration(repeatTimeStr);

        this.charSpamEnabled = config.getBoolean("anti-spam.character-spam.enabled", true);
        this.charLimit = config.getInt("anti-spam.character-spam.char-limit", 5);
        this.wordLimit = config.getInt("anti-spam.character-spam.word-limit", 3);

        this.ipSplitEnabled = config.getBoolean("anti-spam.ip-split-detection.enabled", true);
        this.digitsLimitChat = config.getInt("anti-spam.digits-max-limit.chat-inputs", 10);
        this.digitsLimitWritable = config.getInt("anti-spam.digits-max-limit.writable-inputs", 10);

        this.capsEnabled = config.getBoolean("anti-spam.caps-limit.enabled", true);
        this.capsLimit = config.getInt("anti-spam.caps-limit.limit", 15);

        this.bookMaxPages = config.getInt("anti-spam.book-limits.max-pages", 30);
        this.bookMaxPageChars = config.getInt("anti-spam.book-limits.max-chars-per-page", 256);
        this.bookOpenDelay = config.getInt("anti-spam.book-limits.open-delay", 2);
        this.bookGlobalRateLimit = config.getInt("anti-spam.book-limits.global-rate-limit", 20);
    }

    public boolean isDomainEnabled() {
        return domainEnabled;
    }

    public boolean isIpEnabled() {
        return ipEnabled;
    }

    public boolean isAntiSpamEnabled() {
        return antiSpamEnabled;
    }

    public boolean isAntiFloodEnabled() {
        return antiFloodEnabled;
    }

    public int getAntiFloodMessages() {
        return antiFloodMessages;
    }

    public int getAntiFloodDelay() {
        return antiFloodDelay;
    }

    public boolean isAntiInjectionEnabled() {
        return antiInjectionEnabled;
    }

    public boolean isAntiInjectionGeneric() {
        return antiInjectionGeneric;
    }

    public boolean isAntiInjectionTags() {
        return antiInjectionTags;
    }

    public boolean isAntiInjectionPapi() {
        return antiInjectionPapi;
    }

    public boolean isAntiRepeatEnabled() {
        return antiRepeatEnabled;
    }

    public int getRepeatSimilarity() {
        return repeatSimilarity;
    }

    public int getRepeatHistorySize() {
        return repeatHistorySize;
    }

    public long getRepeatCooldown() {
        return repeatCooldown;
    }

    public boolean isCharSpamEnabled() {
        return charSpamEnabled;
    }

    public int getCharLimit() {
        return charLimit;
    }

    public int getWordLimit() {
        return wordLimit;
    }

    public boolean isIpSplitEnabled() {
        return ipSplitEnabled;
    }

    public int getDigitsLimitChat() {
        return digitsLimitChat;
    }

    public int getDigitsLimitWritable() {
        return digitsLimitWritable;
    }

    public boolean isCapsEnabled() {
        return capsEnabled;
    }

    public int getCapsLimit() {
        return capsLimit;
    }

    public int getBookMaxPages() {
        return bookMaxPages;
    }

    public int getBookMaxPageChars() {
        return bookMaxPageChars;
    }

    public int getBookOpenDelay() {
        return bookOpenDelay;
    }

    public int getBookGlobalRateLimit() {
        return bookGlobalRateLimit;
    }
}
