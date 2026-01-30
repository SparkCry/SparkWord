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
package com.sparkword.commands.impl.suggest;

import com.sparkword.Environment;
import com.sparkword.commands.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SuggestCommand implements SubCommand {

    private final Environment env;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public SuggestCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("This command is only for players.", NamedTextColor.RED));
            return true;
        }

        if (!env.getConfigManager().isUnicodeEnabled()) {
            if (!env.getConfigManager().isSuggestionEnabled()) {
                env.getMessageManager().sendMessage(p, "suggestions.disabled");
                return true;
            }
        }

        if (!p.hasPermission("sparkword.suggest")) {
            env.getMessageManager().sendMessage(p, "no-permission");
            return true;
        }

        if (args.length < 2) {
            env.getMessageManager().sendMessage(p, "help.suggest-usage");
            return true;
        }

        UUID uuid = p.getUniqueId();
        if (!p.hasPermission("sparkword.bypass.cooldown") && cooldowns.containsKey(uuid)) {
            long lastUsage = cooldowns.get(uuid);
            long diff = System.currentTimeMillis() - lastUsage;
            long cd = env.getConfigManager().getSuggestionCooldown() * 1000L;

            if (diff < cd) {
                env.getMessageManager().sendMessage(p, "suggestions.suggest-cooldown", Map.of("time", String.valueOf((cd - diff) / 1000)));
                return true;
            }
        }

        String word = args[0];
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
        String reason = sb.toString().trim();

        int maxWord = env.getConfigManager().getSuggestionMaxWord();
        int maxReason = env.getConfigManager().getSuggestionMaxReason();

        if (word.length() > maxWord) {
            env.getMessageManager().sendMessage(p, "suggestions.long-suggestion", Map.of("limit", String.valueOf(maxWord)));
            return true;
        }
        if (reason.length() > maxReason) {
            env.getMessageManager().sendMessage(p, "suggestion.second-long-suggestion", Map.of("limit", String.valueOf(maxReason)));
            return true;
        }

        env.getStorage().getPlayerIdAsync(p.getUniqueId(), p.getName())
            .thenCompose(pid -> env.getStorage().addSuggestionAsync(pid, word, reason))
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                    if (success) {
                        if (!p.hasPermission("sparkword.bypass.cooldown")) {
                            cooldowns.put(uuid, System.currentTimeMillis());
                        }
                        env.getMessageManager().sendMessage(p, "suggestions.suggest-success");
                        env.getNotifyManager().notifySuggestion(p, word, reason);
                    } else {
                        env.getMessageManager().sendMessage(p, "suggestions.suggest-exists");
                    }
                });
            });

        return true;
    }
}
