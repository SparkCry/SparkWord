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
package com.sparkword.core.storage.cache;

import com.sparkword.SparkWord;
import com.sparkword.core.storage.model.MuteInfo;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MuteCache {

    private final SparkWord plugin;
    private final Map<Integer, MuteInfo> cache = new ConcurrentHashMap<>();
    private final Set<Integer> pendingUnmutes = ConcurrentHashMap.newKeySet();

    public MuteCache(SparkWord plugin) {
        this.plugin = plugin;
    }

    public MuteInfo get(int playerId) {
        MuteInfo info = cache.get(playerId);

        if (info != null && info.hasExpired()) {

            if (pendingUnmutes.add(playerId)) {

                update(playerId, MuteInfo.NOT_MUTED);

                plugin.getEnvironment().getStorage().unmute(playerId)
                    .whenComplete((v, ex) -> {
                        if (ex != null) {
                            plugin.getLogger().warning("Error unmuting ID " + playerId + ": " + ex.getMessage());

                            invalidate(playerId);
                        }
                        pendingUnmutes.remove(playerId);
                    });
            }
            return MuteInfo.NOT_MUTED;
        }

        return info != null ? info : MuteInfo.NOT_MUTED;
    }

    public void update(int playerId, MuteInfo info) {
        cache.put(playerId, info);
        pendingUnmutes.remove(playerId);
    }

    public void remove(int playerId) {
        cache.remove(playerId);
        pendingUnmutes.remove(playerId);
    }

    public void invalidate(int playerId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MuteInfo fresh = plugin.getEnvironment().getStorage().fetchMuteInfoBlocking(playerId);
            update(playerId, fresh);
        });
    }

    public void clear() {
        cache.clear();
        pendingUnmutes.clear();
    }
}
