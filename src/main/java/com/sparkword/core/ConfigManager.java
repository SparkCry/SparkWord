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
import com.sparkword.core.config.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventPriority;

import java.io.File;

public class ConfigManager {
    private final SparkWord plugin;

    private final StorageSettings storageSettings = new StorageSettings();
    private final GeneralSettings generalSettings = new GeneralSettings();
    private final FilterSettings filterSettings = new FilterSettings();
    private final AntiSpamSettings antiSpamSettings = new AntiSpamSettings();
    private final NotificationSettings notificationSettings = new NotificationSettings();
    private final SuggestionSettings suggestionSettings = new SuggestionSettings();
    private final SecuritySettings securitySettings = new SecuritySettings();

    private FileConfiguration moderationConfig;
    private FileConfiguration securityConfig;

    public ConfigManager(SparkWord plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration mainConfig = plugin.getConfig();

        File modFile = new File(plugin.getDataFolder(), "moderation.yml");
        if (!modFile.exists()) {
            plugin.saveResource("moderation.yml", false);
        }
        this.moderationConfig = YamlConfiguration.loadConfiguration(modFile);

        File secFile = new File(plugin.getDataFolder(), "moderation/security-filters.yml");

        if (!secFile.exists()) {
            plugin.saveResource("moderation/security-filters.yml", false);
        }
        this.securityConfig = YamlConfiguration.loadConfiguration(secFile);

        this.generalSettings.load(mainConfig);
        this.storageSettings.load(mainConfig);
        this.suggestionSettings.load(mainConfig);
        this.notificationSettings.load(mainConfig);

        this.filterSettings.load(moderationConfig);
        this.antiSpamSettings.load(moderationConfig);

        this.securitySettings.load(securityConfig);

        plugin.setDebugMode(this.generalSettings.isDebugMode());
    }

    public StorageSettings getStorageSettings() {
        return storageSettings;
    }
    public GeneralSettings getGeneralSettings() {
        return generalSettings;
    }
    public FilterSettings getFilterSettings() {
        return filterSettings;
    }

    public AntiSpamSettings getAntiSpamSettings() {
        return antiSpamSettings;
    }

    public NotificationSettings getNotificationSettings() {
        return notificationSettings;
    }

    public SuggestionSettings getSuggestionSettings() {
        return suggestionSettings;
    }

    public SecuritySettings getSecuritySettings() {
        return securitySettings;
    }

    public boolean isUpdateCheck() {
        return generalSettings.isUpdateCheck();
    }

    public boolean isDebugMode() {
        return generalSettings.isDebugMode();
    }

    public EventPriority getEventPriority() {
        return generalSettings.getEventPriority();
    }

    public String getLocale() {
        return generalSettings.getLocale();
    }

    public String getStorageType() {
        return storageSettings.getStorageType();
    }

    public String getDbHost() {
        return storageSettings.getDbHost();
    }

    public int getDbPort() {
        return storageSettings.getDbPort();
    }

    public String getDbName() {
        return storageSettings.getDbName();
    }

    public String getDbUser() {
        return storageSettings.getDbUser();
    }

    public String getDbPassword() {
        return storageSettings.getDbPassword();
    }

    public int getDbPoolSize() {
        return storageSettings.getDbPoolSize();
    }

    public long getDbMaxLifetime() {
        return storageSettings.getDbMaxLifetime();
    }

    public int getDbTimeout() {
        return storageSettings.getDbTimeout();
    }

    public boolean isUnicodeEnabled() {
        return filterSettings.isUnicodeEnabled();
    }

    public boolean isZalgoEnabled() {
        return filterSettings.isZalgoEnabled();
    }

    public boolean isDomainEnabled() {
        return antiSpamSettings.isDomainEnabled();
    }

    public boolean isIpEnabled() {
        return antiSpamSettings.isIpEnabled();
    }

    public boolean isFilterChat() {
        return filterSettings.isFilterChat();
    }

    public boolean isFilterSigns() {
        return filterSettings.isFilterSigns();
    }

    public boolean isFilterBooks() {
        return filterSettings.isFilterBooks();
    }

    public boolean isFilterAnvils() {
        return filterSettings.isFilterAnvils();
    }

    public int getBookMaxPages() {
        return antiSpamSettings.getBookMaxPages();
    }

    public int getBookMaxPageChars() {
        return antiSpamSettings.getBookMaxPageChars();
    }

    public int getBookOpenDelay() {
        return antiSpamSettings.getBookOpenDelay();
    }

    public int getBookGlobalRateLimit() {
        return antiSpamSettings.getBookGlobalRateLimit();
    }

    public boolean isAntiSpamEnabled() {
        return antiSpamSettings.isAntiSpamEnabled();
    }

    public boolean isAntiFloodEnabled() {
        return antiSpamSettings.isAntiFloodEnabled();
    }

    public int getAntiFloodMessages() {
        return antiSpamSettings.getAntiFloodMessages();
    }

    public int getAntiFloodDelay() {
        return antiSpamSettings.getAntiFloodDelay();
    }

    public boolean isAntiInjectionEnabled() {
        return antiSpamSettings.isAntiInjectionEnabled();
    }

    public boolean isAntiInjectionGeneric() {
        return antiSpamSettings.isAntiInjectionGeneric();
    }

    public boolean isAntiInjectionTags() {
        return antiSpamSettings.isAntiInjectionTags();
    }

    public boolean isAntiInjectionPapi() {
        return antiSpamSettings.isAntiInjectionPapi();
    }

    public boolean isAntiRepeatEnabled() {
        return antiSpamSettings.isAntiRepeatEnabled();
    }

    public int getRepeatSimilarity() {
        return antiSpamSettings.getRepeatSimilarity();
    }

    public int getRepeatHistorySize() {
        return antiSpamSettings.getRepeatHistorySize();
    }

    public long getRepeatCooldown() {
        return antiSpamSettings.getRepeatCooldown();
    }

    public boolean isCharSpamEnabled() {
        return antiSpamSettings.isCharSpamEnabled();
    }

    public int getCharLimit() {
        return antiSpamSettings.getCharLimit();
    }

    public int getWordLimit() {
        return antiSpamSettings.getWordLimit();
    }

    public boolean isIpSplitEnabled() {
        return antiSpamSettings.isIpSplitEnabled();
    }

    public int getDigitsLimitChat() {
        return antiSpamSettings.getDigitsLimitChat();
    }

    public int getDigitsLimitWritable() {
        return antiSpamSettings.getDigitsLimitWritable();
    }

    public boolean isReplacementEnabled() {
        return filterSettings.isReplacementEnabled();
    }

    public String getGlobalReplacement() {
        return filterSettings.getGlobalReplacement();
    }

    public boolean isNotifyIconEnabled() {
        return notificationSettings.isNotifyIconEnabled();
    }

    public boolean isNotifyTypeFilter() {
        return notificationSettings.isNotifyTypeFilter();
    }

    public boolean isNotifyTypeFlood() {
        return notificationSettings.isNotifyTypeFlood();
    }

    public boolean isNotifyTypeIp() {
        return notificationSettings.isNotifyTypeIp();
    }

    public boolean isNotifyTypeZalgo() {
        return notificationSettings.isNotifyTypeZalgo();
    }

    public boolean isNotifyTypeInjection() {
        return notificationSettings.isNotifyTypeInjection();
    }

    public boolean isSuggestionEnabled() {
        return suggestionSettings.isSuggestionEnabled();
    }

    public int getSuggestionCooldown() {
        return suggestionSettings.getSuggestionCooldown();
    }

    public int getSuggestionMaxWord() {
        return suggestionSettings.getSuggestionMaxWord();
    }

    public int getSuggestionMaxReason() {
        return suggestionSettings.getSuggestionMaxReason();
    }
}
