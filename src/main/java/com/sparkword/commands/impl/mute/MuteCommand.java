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
package com.sparkword.commands.impl.mute;

import com.sparkword.Environment;
import com.sparkword.commands.SubCommand;
import com.sparkword.core.storage.model.MuteInfo;
import com.sparkword.util.PaperProfileUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MuteCommand implements SubCommand {
    private final Environment env;

    public MuteCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.mute")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            env.getMessageManager().sendMessage(sender, "help.usage-mute");
            return true;
        }

        String playerName = args[0];
        String reasonRaw = env.getMessageManager().getString("moderation.default.no-reason");

        if (args.length > 1) {
            String maybePreset = args[1];
            String presetValue = env.getConfigManager().getGeneralSettings().getPreset(maybePreset);

            if (presetValue != null) {
                reasonRaw = presetValue;

                if (args.length > 2) {
                    reasonRaw += " " + String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                }
            } else {
                reasonRaw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }
        final String reason = reasonRaw;

        // Try DB lookup first, then fallback to Mojang API
        env.getStorage().getPlayerIdByNameAsync(playerName).thenCompose(dbId -> {
            if (dbId != -1) return CompletableFuture.completedFuture(Map.entry(dbId, playerName));

            return PaperProfileUtil.resolve(playerName).thenCompose(target -> {
                if (target == null) return CompletableFuture.completedFuture(null);
                String resolvedName = target.getName() != null ? target.getName() : playerName;
                return env.getStorage().getPlayerIdAsync(target.getUniqueId(), resolvedName)
                    .thenApply(id -> Map.entry(id, resolvedName));
            });
        }).thenAcceptAsync(entry -> {
            if (entry == null) {
                env.getMessageManager().sendMessage(sender, "player-not-found");
                return;
            }

            int pid = entry.getKey();
            String name = entry.getValue();

            if (pid != -1) {
                env.getStorage().mute(pid, reason, sender.getName(), 0, "MUTE", MuteInfo.MuteScope.CHAT, success -> {
                    Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                        String timeText = env.getMessageManager().getString("moderation.default.mute");
                        env.getMessageManager().sendMessage(sender, "moderation.mute-success", Map.of("player", name, "time", timeText));

                        Player onlineTarget = Bukkit.getPlayerExact(name);
                        if (onlineTarget != null) {
                            env.getMessageManager().sendMessage(onlineTarget, "moderation.player-muted", Map.of("staff", sender.getName(), "reason", reason, "time", timeText));
                        }
                    });
                });
            } else {
                Bukkit.getScheduler().runTask(env.getPlugin(), () -> env.getMessageManager().sendMessage(sender, "error.register-player"));
            }
        }, Bukkit.getScheduler().getMainThreadExecutor(env.getPlugin()));

        return true;
    }
}
