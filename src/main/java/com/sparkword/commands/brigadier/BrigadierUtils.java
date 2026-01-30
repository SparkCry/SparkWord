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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sparkword.SparkWord;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BrigadierUtils {

    public static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        Bukkit.getOnlinePlayers().stream()
            .map(org.bukkit.entity.Player::getName)
            .filter(name -> remaining.isEmpty() || name.toLowerCase().startsWith(remaining))
            .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggestPresets(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        SparkWord plugin = SparkWord.getInstance();
        if (plugin != null && plugin.getConfig().isConfigurationSection("presets")) {
            plugin.getConfig().getConfigurationSection("presets").getKeys(false).forEach(builder::suggest);
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggestDurations(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        List.of("15m", "30m", "1h", "3h", "12h", "1d", "3d", "7d", "30d").forEach(builder::suggest);
        return builder.buildFuture();
    }
}
