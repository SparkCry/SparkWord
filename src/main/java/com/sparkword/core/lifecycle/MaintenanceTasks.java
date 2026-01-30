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
import org.bukkit.scheduler.BukkitTask;

public class MaintenanceTasks {

    private final SparkWord plugin;
    private final StorageManager storage;
    private BukkitTask purgeTask;

    public MaintenanceTasks(SparkWord plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void startPurgeTask() {
        stopPurgeTask();

        long purgeHours = plugin.getConfig().getInt("suggestion.purge-hours", 72);
        if (purgeHours > 0 && storage != null) {
            this.purgeTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                () -> storage.purgeData("sg", purgeHours / 24),
                1200L,
                72000L
            );
        }
    }

    public void stopPurgeTask() {
        if (purgeTask != null && !purgeTask.isCancelled()) {
            purgeTask.cancel();
            purgeTask = null;
        }
    }

    public void stopAll() {
        stopPurgeTask();
    }
}
