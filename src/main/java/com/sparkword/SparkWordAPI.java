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
package com.sparkword;

import com.sparkword.core.storage.StorageManager;
import com.sparkword.core.storage.model.MuteInfo.MuteScope;
import com.sparkword.moderation.filters.word.result.FilterResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SparkWordAPI {

    private static SparkWord plugin;

    public SparkWordAPI(SparkWord instance) {
        plugin = instance;
    }

    public static boolean isClean(String text) {
        if (plugin == null || !plugin.isEnabled()) return true;
        return !plugin.getFilterManager().processText(text, false, null).blocked();
    }

    public static boolean isClean(String text, Player player) {
        if (plugin == null || !plugin.isEnabled()) return true;
        return !plugin.getFilterManager().processText(text, false, player).blocked();
    }

    public static boolean isCleanForCommand(String text) {
        if (plugin == null || !plugin.isEnabled()) return true;
        FilterResult result = plugin.getFilterManager().processWriteCommand(text);
        return !result.blocked();
    }

    public static String filterText(String text) {
        if (plugin == null || !plugin.isEnabled()) return text;
        FilterResult result = plugin.getFilterManager().processText(text, false, null);

        if (result.blocked()) return plugin.getEnvironment().getConfigManager().getGlobalReplacement();
        return result.processedMessage() != null ? result.processedMessage() : text;
    }

    public static String filterText(String text, Player player) {
        if (plugin == null || !plugin.isEnabled()) return text;
        FilterResult result = plugin.getFilterManager().processText(text, false, player);

        if (result.blocked()) return plugin.getEnvironment().getConfigManager().getGlobalReplacement();
        return result.processedMessage() != null ? result.processedMessage() : text;
    }

    public static Component filterComponent(Component text, Player player) {
        if (plugin == null || !plugin.isEnabled()) return text;

        String plain = PlainTextComponentSerializer.plainText().serialize(text);
        FilterResult result = plugin.getFilterManager().processText(plain, false, player);

        if (result.blocked()) {
            return Component.text(plugin.getEnvironment().getConfigManager().getGlobalReplacement());
        }

        if (result.processedMessage() != null && !result.processedMessage().equals(plain)) {
            return Component.text(result.processedMessage());
        }

        return text;
    }

    public static void mutePlayer(UUID uuid, String name, int seconds, String reason, boolean global) {
        if (plugin == null || !plugin.isEnabled()) return;

        plugin.getEnvironment().getStorage().getPlayerIdAsync(uuid, name).thenAccept(id -> {
            if (id != -1) {
                MuteScope scope = global ? MuteScope.GLOBAL : MuteScope.CHAT;
                plugin.getEnvironment().getStorage().mute(id, reason, StorageManager.SYSTEM_ACTOR, seconds, "API_MUTE", scope, null);
            } else {
                plugin.getLogger().warning("API: Could not register/find player " + name + " for mute.");
            }
        });
    }
}
