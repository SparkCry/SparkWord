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
package com.sparkword.listeners;

import com.sparkword.SparkWord;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class ClientBrandListener implements Listener, PluginMessageListener {

    private final SparkWord plugin;

    public ClientBrandListener(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {

        if (!channel.equals("minecraft:brand") && !channel.equals("MC|Brand")) return;

        try {

            String brand = new String(message, StandardCharsets.UTF_8).trim();

            plugin.getEnvironment().getSecurityManager().checkClientBrand(player, brand);

        } catch (Exception e) {
            plugin.getLogger().warning("Error reading Brand of " + player.getName());
        }
    }
}
