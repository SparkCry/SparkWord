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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventPriority;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GeneralSettings {

    private final Map<String, String> presets = new HashMap<>();
    private boolean updateCheck;
    private boolean debugMode;
    private EventPriority eventPriority;
    private String locale;
    private int historyPlayerDays;

    public GeneralSettings() {
    }

    public void load(FileConfiguration config) {
        this.updateCheck = config.getBoolean("update-check", true);
        this.debugMode = config.getBoolean("debug", false);
        try {
            this.eventPriority = EventPriority.valueOf(config.getString("event-priority", "HIGH").toUpperCase());
        } catch (Exception e) {
            this.eventPriority = EventPriority.HIGH;
        }
        this.locale = config.getString("locale", "en");
        this.historyPlayerDays = config.getInt("history-player", 7);

        this.presets.clear();
        ConfigurationSection section = config.getConfigurationSection("presets");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String value = section.getString(key);
                if (value != null) {
                    this.presets.put(key.toLowerCase(), value);
                }
            }
        }
    }

    public boolean isUpdateCheck() {
        return updateCheck;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public EventPriority getEventPriority() {
        return eventPriority;
    }

    public String getLocale() {
        return locale;
    }

    public int getHistoryPlayerDays() {return historyPlayerDays;}

    public String getPreset(String key) {
        if (key == null) return null;
        return presets.get(key.toLowerCase());
    }

    public boolean hasPreset(String key) {
        if (key == null) return false;
        return presets.containsKey(key.toLowerCase());
    }

    public Set<String> getPresetKeys() {
        return presets.keySet();
    }
}
