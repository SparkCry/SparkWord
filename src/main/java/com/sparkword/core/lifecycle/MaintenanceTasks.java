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
package com.sparkword.core.lifecycle;

import com.sparkword.SparkWord;
import com.sparkword.core.storage.StorageManager;
import org.bukkit.Bukkit;

public class MaintenanceTasks {

    private final SparkWord plugin;
    private final StorageManager storage;
    private boolean hasRunStartupPurge = false;

    public MaintenanceTasks(SparkWord plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void startPurgeTask() {
        if (hasRunStartupPurge) {
            return;
        }
        hasRunStartupPurge = true;

        long purgeHours = plugin.getConfig().getInt("suggestion.purge-hours", 72);

        int historyDays = plugin.getEnvironment().getConfigManager().getGeneralSettings().getHistoryPlayerDays();

        if ((purgeHours > 0 || historyDays > 0) && storage != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                int totalDeleted = 0;

                if (purgeHours > 0) {
                    totalDeleted += storage.purgeData("sg", purgeHours / 24);
                }
                if (historyDays > 0) {
                    totalDeleted += storage.purgeData("hp", historyDays);
                }

                if (plugin.isDebugMode() && totalDeleted > 0) {
                    plugin.getLogger().info("Startup purge complete. Removed " + totalDeleted + " expired records.");
                }
            });
        }
    }

    public void stopPurgeTask() {
    }

    public void stopAll() {
    }
}
