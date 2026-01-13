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
package com.sparkword.data;

import com.sparkword.SparkWord;
import com.sparkword.model.MuteInfo;

import java.util.UUID;

public class PlayerLoginSync {

    private final SparkWord plugin;
    private final MuteCache muteCache;

    public PlayerLoginSync(SparkWord plugin, MuteCache muteCache) {
        this.plugin = plugin;
        this.muteCache = muteCache;
    }

    public int handleLogin(UUID uuid, String name) {
        try {

            int playerId = plugin.getEnvironment().getStorage().getPlayerIdBlocking(uuid, name);

            if (playerId != -1) {

                MuteInfo info = plugin.getEnvironment().getStorage().fetchMuteInfoBlocking(playerId);
                muteCache.update(playerId, info);
            }

            return playerId;
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading player data " + name + ": " + e.getMessage());
            return -1;
        }
    }
}
