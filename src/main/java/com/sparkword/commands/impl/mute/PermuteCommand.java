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

import com.sparkword.commands.SubCommand;
import com.sparkword.core.Environment;
import com.sparkword.model.MuteInfo.MuteScope;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PermuteCommand implements SubCommand {
    private final Environment env;
    public PermuteCommand(Environment env) { this.env = env; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.permute")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            env.getMessageManager().sendMessage(sender, "usage-mute");
            return true;
        }

        String reasonRaw = "No reason";
        if (args.length > 1) {
            String maybePreset = args[1];
            if (env.getPlugin().getConfig().contains("presets." + maybePreset)) {
                reasonRaw = env.getPlugin().getConfig().getString("presets." + maybePreset);
                if (args.length > 2) {
                    reasonRaw += " " + String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                }
            } else {
                reasonRaw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }

        final String reason = reasonRaw;
        final String targetName = args[0];

        env.getMessageManager().sendMessage(sender, "defaults.processing");

        CompletableFuture.supplyAsync(() -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer p = Bukkit.getOfflinePlayer(targetName);
            return p;
        }, env.getAsyncExecutor()).thenCompose(target -> {

            return env.getStorage().getPlayerIdAsync(target.getUniqueId(), target.getName() != null ? target.getName() : targetName)
                .thenApply(id -> Map.entry(target, id));
        }).thenAcceptAsync(entry -> {
            OfflinePlayer target = entry.getKey();
            int pid = entry.getValue();

            if (pid != -1) {

                env.getStorage().mute(pid, reason, sender.getName(), 0, "PERMUTE", MuteScope.GLOBAL, success -> {
                    Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                        env.getMessageManager().sendMessage(sender, "mute-success", Map.of("player", target.getName() != null ? target.getName() : "Unknown", "time", "Permanent (TOTAL)"));
                        if (target.isOnline() && target.getPlayer() != null) {
                            env.getMessageManager().sendMessage(target.getPlayer(), "player-muted", Map.of("staff", sender.getName(), "reason", reason, "time", "Permanent (TOTAL)"));
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
