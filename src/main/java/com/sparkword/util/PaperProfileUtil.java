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
package com.sparkword.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PaperProfileUtil {

    private PaperProfileUtil() {
    }

    /**
     * Asynchronously resolves an OfflinePlayer by name using Paper's PlayerProfile API.
     * Prioritizes online players for immediate resolution.
     *
     * @param name The player name to resolve.
     * @return A CompletableFuture containing the OfflinePlayer if resolved, or null if not found.
     */
    public static CompletableFuture<OfflinePlayer> resolve(String name) {
        // 1. Check online players first (Fast, Sync, Reliable)
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return CompletableFuture.completedFuture(online);
        }

        // 2. Async Profile Lookup (Fallback for offline players)
        Server server = Bukkit.getServer();
        PlayerProfile profile = server.createPlayerProfile(name);

        return profile.update().thenApply(updatedProfile -> {
            UUID uuid = updatedProfile.getUniqueId();
            return uuid != null ? Bukkit.getOfflinePlayer(uuid) : null;
        }).exceptionally(ex -> {
            // Handle API failures (rate limits, network issues) gracefully
            return null;
        });
    }
}
