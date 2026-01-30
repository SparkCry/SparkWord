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
import com.sparkword.core.ConfigManager;
import com.sparkword.util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class BootstrapIntegrations {

    private static final int BSTATS_ID = 28822;
    private final SparkWord plugin;

    public BootstrapIntegrations(SparkWord plugin) {
        this.plugin = plugin;
    }

    public void initMetrics(ConfigManager config) {
        Metrics metrics = new Metrics(plugin, BSTATS_ID);

        // CHANGED: Accessing settings via modules
        metrics.addCustomChart(new SimplePie("database_type", () -> "SQLite")); // Dynamic check could use config.getStorageSettings().getStorageType()
        metrics.addCustomChart(new SimplePie("anti_flood_enabled", () ->
            config.getAntiSpamSettings().isAntiFloodEnabled() ? "Yes" : "No"));
    }

    public void checkUpdates() {
        UpdateChecker updater = new UpdateChecker(plugin);
        updater.check();
        plugin.getServer().getPluginManager().registerEvents(updater, plugin);
    }
}
