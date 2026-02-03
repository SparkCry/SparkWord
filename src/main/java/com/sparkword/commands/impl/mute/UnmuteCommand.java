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
import com.sparkword.util.PaperProfileUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UnmuteCommand implements SubCommand {
    private final Environment env;

    public UnmuteCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.unmute")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            env.getMessageManager().sendMessage(sender, "help.usage-unmute");
            return true;
        }

        String reason = "Manual";
        if (args.length > 1) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
        final String fReason = reason;
        final String targetName = args[0];

        env.getStorage().getPlayerIdByNameAsync(targetName).thenCompose(dbId -> {
            if (dbId != -1) return CompletableFuture.completedFuture(Map.entry(dbId, targetName));

            return PaperProfileUtil.resolve(targetName).thenCompose(target -> {
                if (target == null) return CompletableFuture.completedFuture(null);
                String resolvedName = target.getName() != null ? target.getName() : targetName;
                return env.getStorage().getPlayerIdAsync(target.getUniqueId(), resolvedName)
                    .thenApply(id -> Map.entry(id, resolvedName));
            });
        }).thenAccept(entry -> {
            if (entry != null && entry.getKey() != -1) {
                env.getStorage().unmute(entry.getKey(), sender.getName(), fReason).thenRun(() -> {
                    Bukkit.getScheduler().runTask(env.getPlugin(), () ->
                            env.getMessageManager().sendMessage(sender, "moderation.unmute-success", Map.of("player", entry.getValue()))
                                                 );
                });
            } else {
                Bukkit.getScheduler().runTask(env.getPlugin(), () ->
                        env.getMessageManager().sendMessage(sender, "player-not-found")
                                             );
            }
        });

        return true;
    }
}
