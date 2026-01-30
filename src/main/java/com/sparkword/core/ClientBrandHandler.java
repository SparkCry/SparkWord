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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ClientBrandHandler implements Listener, PluginMessageListener {

    private final SparkWord plugin;
    // Merged from SecurityManager: pattern to sanitize brand names
    private final Pattern CLEAN_BRAND = Pattern.compile("[^a-zA-Z0-9_\\-: ]");

    public ClientBrandHandler(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals("minecraft:brand") && !channel.equals("MC|Brand")) return;

        try {
            String rawBrand = new String(message, StandardCharsets.UTF_8).trim();
            String processedBrand = sanitizeBrand(player, rawBrand);

            // Store the processed brand in PlayerDataManager
            if (plugin.getEnvironment().getPlayerDataManager() != null) {
                plugin.getEnvironment().getPlayerDataManager().setClientBrand(player.getUniqueId(), processedBrand);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error reading Brand of " + player.getName());
        }
    }

    /**
     * Logic previously found in SecurityManager.
     * Validates and cleans the brand string.
     */
    private String sanitizeBrand(Player player, String brand) {
        if (brand == null) return "Unknown";

        // Remove special characters to prevent chat breaking or log injection
        String normalized = CLEAN_BRAND.matcher(brand).replaceAll("");
        String check = normalized.toLowerCase().trim();

        // Heuristic checks
        if (check.length() > 20 || check.length() < 2) {
            // We store it as "Unknown" or a safe fallback if it looks suspicious
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Heuristic] Suspicious Brand (" + player.getName() + "): " + brand);
            }
            return "Unknown";
        }

        // Return the clean, normalized string
        return normalized;
    }

    // Optional: Ensure vanilla players are marked if no packet is received (though usually they send "vanilla")
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Default to vanilla/unknown until the packet arrives (usually milliseconds after join)
        if (plugin.getEnvironment().getPlayerDataManager() != null) {
            plugin.getEnvironment().getPlayerDataManager().setClientBrand(event.getPlayer().getUniqueId(), "vanilla");
        }
    }
}
