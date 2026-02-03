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
package com.sparkword.commands.brigadier;

import com.sparkword.SparkWord;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AliasHandler {

    private final SparkWord plugin;
    private final Map<String, String> aliasToRealMap = new HashMap<>();

    public AliasHandler(SparkWord plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        aliasToRealMap.clear();
        File file = new File(plugin.getDataFolder(), "commands.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.isConfigurationSection("commands.alias")) {
            for (String realCmd : config.getConfigurationSection("commands.alias").getKeys(false)) {
                List<String> aliases = config.getStringList("commands.alias." + realCmd);
                for (String alias : aliases) {
                    aliasToRealMap.put(alias.toLowerCase(), realCmd.toLowerCase());
                }

                String single = config.getString("commands.alias." + realCmd);
                if (single != null && aliases.isEmpty()) {
                    aliasToRealMap.put(single.toLowerCase(), realCmd.toLowerCase());
                }
            }
        }
    }

    public String resolve(String label) {
        if (label == null) return null;
        String lower = label.toLowerCase();

        int depth = 0;
        while (aliasToRealMap.containsKey(lower) && depth++ < 5) {
            lower = aliasToRealMap.get(lower);
        }
        return lower;
    }

    public Map<String, String> getAliasMap() {
        return aliasToRealMap;
    }
}
