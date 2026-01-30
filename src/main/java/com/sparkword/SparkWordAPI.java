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

/**
 * Public API for SparkWord.
 * Allows other plugins to interact with the filtering and moderation system.
 */
public class SparkWordAPI {

    private static SparkWord plugin;

    /**
     * Internal constructor. Do not initialize this class manually.
     * SparkWord initializes this on enable.
     */
    public SparkWordAPI(SparkWord instance) {
        plugin = instance;
    }

    /**
     * Checks if a text string contains any blocked words or violations.
     * Does not check for player-specific permissions.
     *
     * @param text The text to check.
     * @return true if the text is clean, false if it contains blocked content.
     */
    public static boolean isClean(String text) {
        if (plugin == null || !plugin.isEnabled()) return true;
        return !plugin.getFilterManager().processText(text, false, null).blocked();
    }

    /**
     * Checks if a text string contains any blocked words or violations,
     * respecting the player's bypass permissions.
     *
     * @param text   The text to check.
     * @param player The player sending the text (used for permission checks).
     * @return true if the text is clean (or player has bypass), false otherwise.
     */
    public static boolean isClean(String text, Player player) {
        if (plugin == null || !plugin.isEnabled()) return true;
        return !plugin.getFilterManager().processText(text, false, player).blocked();
    }

    /**
     * Checks if a command argument or string violates the "Write Command" filter list.
     * Useful for checking command inputs like /msg or /rename.
     *
     * @param text The command text or argument to check.
     * @return true if safe, false if blocked.
     */
    public static boolean isCleanForCommand(String text) {
        if (plugin == null || !plugin.isEnabled()) return true;
        FilterResult result = plugin.getFilterManager().processWriteCommand(text);
        return !result.blocked();
    }

    /**
     * Filters a string and returns the clean version.
     * If blocked entirely, returns the global replacement string (e.g. "****").
     * If partially blocked (replacements), returns the modified string.
     *
     * @param text The raw text.
     * @return The filtered text.
     */
    public static String filterText(String text) {
        if (plugin == null || !plugin.isEnabled()) return text;
        FilterResult result = plugin.getFilterManager().processText(text, false, null);

        if (result.blocked()) return plugin.getEnvironment().getConfigManager().getGlobalReplacement();
        return result.processedMessage() != null ? result.processedMessage() : text;
    }

    /**
     * Filters a string for a specific player, respecting bypass permissions.
     *
     * @param text   The raw text.
     * @param player The player context.
     * @return The filtered text.
     */
    public static String filterText(String text, Player player) {
        if (plugin == null || !plugin.isEnabled()) return text;
        FilterResult result = plugin.getFilterManager().processText(text, false, player);

        if (result.blocked()) return plugin.getEnvironment().getConfigManager().getGlobalReplacement();
        return result.processedMessage() != null ? result.processedMessage() : text;
    }

    /**
     * Filters an Adventure Component.
     * Note: This serializes the component to plain text for checking.
     *
     * @param text   The component to filter.
     * @param player The player context.
     * @return A new Component if filtered, or the original if clean.
     */
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

    /**
     * Applies a mute to a player via the API.
     * This method is asynchronous.
     *
     * @param uuid    The UUID of the player.
     * @param name    The name of the player (used for DB lookup/registration).
     * @param seconds Duration in seconds (0 for permanent).
     * @param reason  The reason for the mute.
     * @param global  If true, mutes globally (chat + commands/actions). If false, chat only.
     */
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
