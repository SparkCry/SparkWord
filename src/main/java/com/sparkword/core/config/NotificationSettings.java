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

public class NotificationSettings {
    private boolean notifyIconEnabled;
    private boolean notifyTypeFilter;
    private boolean notifyTypeFlood;
    private boolean notifyTypeIp;
    private boolean notifyTypeZalgo;
    private boolean notifyTypeInjection;

    public NotificationSettings() {
    }

    public void load(FileConfiguration config) {
        this.notifyIconEnabled = config.getBoolean("notifications.icon-hover.enabled", true);
        this.notifyTypeFilter = config.getBoolean("notifications.types.filter", true);
        this.notifyTypeFlood = config.getBoolean("notifications.types.flood", true);
        this.notifyTypeIp = config.getBoolean("notifications.types.ip", true);
        this.notifyTypeZalgo = config.getBoolean("notifications.types.zalgo", true);
        this.notifyTypeInjection = config.getBoolean("notifications.types.injection", true);
    }

    public boolean isNotifyIconEnabled() {
        return notifyIconEnabled;
    }

    public boolean isNotifyTypeFilter() {
        return notifyTypeFilter;
    }

    public boolean isNotifyTypeFlood() {
        return notifyTypeFlood;
    }

    public boolean isNotifyTypeIp() {
        return notifyTypeIp;
    }

    public boolean isNotifyTypeZalgo() {
        return notifyTypeZalgo;
    }

    public boolean isNotifyTypeInjection() {
        return notifyTypeInjection;
    }
}
