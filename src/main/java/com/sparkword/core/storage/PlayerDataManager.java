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
package com.sparkword.core.storage;

import com.sparkword.SparkWord;
import com.sparkword.core.storage.cache.MuteCache;
import com.sparkword.core.storage.cache.PlayerLoginSync;
import com.sparkword.core.storage.model.MuteInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager implements Listener {

    private final SparkWord plugin;

    private final MuteCache muteCache;
    private final PlayerLoginSync loginSync;

    private final ConcurrentHashMap<UUID, Integer> onlinePlayerIds = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, String> clientBrands = new ConcurrentHashMap<>();

    public PlayerDataManager(SparkWord plugin) {
        this.plugin = plugin;

        this.muteCache = new MuteCache(plugin);
        this.loginSync = new PlayerLoginSync(plugin, muteCache);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        int dbId = loginSync.handleLogin(event.getUniqueId(), event.getName());

        if (dbId != -1) {
            onlinePlayerIds.put(event.getUniqueId(), dbId);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Integer id = onlinePlayerIds.remove(uuid);

        if (id != null) {
            muteCache.remove(id);
        }

        clientBrands.remove(uuid);

        if (plugin.getSpamManager() != null) {
            plugin.getSpamManager().cleanupPlayer(uuid);
        }
    }

    /**
     * Re-initializes data for an online player. Used during reloads to ensure
     * mutes are re-applied and IDs are re-fetched without requiring a re-login.
     */
    public void refreshPlayer(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int dbId = loginSync.handleLogin(player.getUniqueId(), player.getName());
            if (dbId != -1) {
                onlinePlayerIds.put(player.getUniqueId(), dbId);
            }
        });
    }

    public void setClientBrand(UUID uuid, String brand) {
        this.clientBrands.put(uuid, brand);
    }

    public String getClientBrand(UUID uuid) {
        return clientBrands.getOrDefault(uuid, "vanilla");
    }

    public int getPlayerId(UUID uuid, String name) {
        return onlinePlayerIds.getOrDefault(uuid, -1);
    }

    public MuteInfo getMuteInfo(int playerId) {
        return muteCache.get(playerId);
    }

    public void invalidateMute(int playerId) {
        muteCache.invalidate(playerId);
    }

    public void updateMuteDirectly(int playerId, MuteInfo info) {
        muteCache.update(playerId, info);
    }

    public void invalidateAll() {
        onlinePlayerIds.clear();
        muteCache.clear();
        clientBrands.clear();
    }
}
