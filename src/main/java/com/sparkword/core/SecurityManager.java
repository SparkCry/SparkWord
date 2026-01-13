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
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class SecurityManager {

    private final SparkWord plugin;
    private final Pattern CLEAN_BRAND = Pattern.compile("[^a-zA-Z0-9_\\-: ]");

    public SecurityManager(SparkWord plugin) {
        this.plugin = plugin;
    }

    public boolean checkClientBrand(Player player, String brand) {
        if (brand == null) return true;

        String normalized = CLEAN_BRAND.matcher(brand).replaceAll("").toLowerCase().trim();

        if (normalized.length() > 20 || normalized.length() < 2) {

            plugin.getLogger().warning("[Heuristic] Suspicious Brand (" + player.getName() + "): " + brand);
            return false;
        }

        return true;
    }
}
