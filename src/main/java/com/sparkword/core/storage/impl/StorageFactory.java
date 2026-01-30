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
package com.sparkword.core.storage.impl;

import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.config.StorageSettings;
import com.sparkword.core.storage.impl.sql.provider.MySQLProvider;
import com.sparkword.core.storage.impl.sql.provider.SQLiteProvider;
import com.sparkword.core.storage.spi.StorageProvider;

public class StorageFactory {

    private final SparkWord plugin;

    public StorageFactory(SparkWord plugin) {
        this.plugin = plugin;
    }

    public StorageProvider createProvider(ConfigManager config) {
        // Use the new modular settings object
        StorageSettings settings = config.getStorageSettings();
        String type = settings.getStorageType();

        if (type.equalsIgnoreCase("mysql") || type.equalsIgnoreCase("mariadb")) {
            return new MySQLProvider(plugin);
        }

        return new SQLiteProvider(plugin);
    }
}
