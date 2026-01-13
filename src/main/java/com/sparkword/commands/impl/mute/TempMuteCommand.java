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
import com.sparkword.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TempMuteCommand implements SubCommand {
    private final Environment env;
    public TempMuteCommand(Environment env) { this.env = env; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.tempmute")) {
             env.getMessageManager().sendMessage(sender, "no-permission");
             return true;
        }
        if (args.length < 2) {
             env.getMessageManager().sendMessage(sender, "usage-tempmute");
             return true;
        }

        long sec = TimeUtil.parseDuration(args[1]);
        if (sec <= 0) {
            env.getMessageManager().sendMessage(sender, "error-invalid-time");
            return true;
        }

        String playerName = args[0];
        String reasonRaw = "No reason";

        if (args.length > 2) {
            String maybePreset = args[2];
            if (env.getPlugin().getConfig().contains("presets." + maybePreset)) {
                reasonRaw = env.getPlugin().getConfig().getString("presets." + maybePreset);
                if (args.length > 3) reasonRaw += " " + String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            } else {
                reasonRaw = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        }
        final String reason = reasonRaw;

        env.getMessageManager().sendMessage(sender, "defaults.processing");

        CompletableFuture.supplyAsync(() -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
            return p;
        }, env.getAsyncExecutor()).thenAcceptAsync(target -> {
            int cachedId = env.getPlayerDataManager().getPlayerId(target.getUniqueId(), target.getName());
            int pid = (cachedId != -1) ? cachedId : env.getStorage().getPlayerIdAsync(target.getUniqueId(), playerName).join();

            if (pid != -1) {

                env.getStorage().mute(pid, reason, sender.getName(), sec, "TEMPMUTE", MuteScope.CHAT, success -> {
                    Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                        String time = TimeUtil.formatDuration(sec);
                        env.getMessageManager().sendMessage(sender, "mute-success", Map.of("player", target.getName() != null ? target.getName() : "Unknown", "time", time));
                        if (target.isOnline() && target.getPlayer() != null) {
                            env.getMessageManager().sendMessage(target.getPlayer(), "player-muted", Map.of("staff", sender.getName(), "reason", reason, "time", time));
                        }
                    });
                });
            } else {
                Bukkit.getScheduler().runTask(env.getPlugin(), () -> env.getMessageManager().sendMessage(sender, "error.db-error"));
            }
        }, Bukkit.getScheduler().getMainThreadExecutor(env.getPlugin()));

        return true;
    }
}
