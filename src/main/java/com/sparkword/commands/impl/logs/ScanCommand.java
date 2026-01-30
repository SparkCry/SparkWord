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
package com.sparkword.commands.impl.logs;

import com.sparkword.Environment;
import com.sparkword.commands.SubCommand;
import com.sparkword.util.PaperProfileUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ScanCommand implements SubCommand {
    private final Environment env;

    public ScanCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.scan")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            env.getMessageManager().sendMessage(sender, "help.usage-scan");
            return true;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (Exception ignored) {
            }
        }
        if (page < 1) page = 1;

        final int fPage = page;
        final String targetName = args[0];

        env.getStorage().getPlayerIdByNameAsync(targetName).thenCompose(dbId -> {
            if (dbId != -1) return CompletableFuture.completedFuture(Map.entry(dbId, targetName));

            return PaperProfileUtil.resolve(targetName).thenCompose(target -> {
                if (target == null) return CompletableFuture.completedFuture(null);
                String resolvedName = target.getName() != null ? target.getName() : targetName;
                return env.getStorage().getPlayerIdAsync(target.getUniqueId(), resolvedName)
                    .thenApply(id -> Map.entry(id, resolvedName));
            });
        }).thenCompose(entry -> {
            if (entry == null || entry.getKey() == -1) {
                throw new RuntimeException("Player not found");
            }
            return env.getStorage().getPlayerReportAsync(entry.getKey(), fPage)
                .thenApply(lines -> Map.entry(entry.getValue(), lines));
        }).thenAccept(result -> {
            Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<dark_gray>--- <#09bbf5>Scan: " + result.getKey() + " (Pg " + fPage + ")</#09bbf5> <dark_gray>---"
                ));

                if (result.getValue().isEmpty()) {
                    env.getMessageManager().sendMessage(sender, "moderation.scan-empty");
                } else {
                    for (String str : result.getValue()) sender.sendMessage(str);
                }
            });
        }).exceptionally(e -> {
            Bukkit.getScheduler().runTask(env.getPlugin(), () ->
                env.getMessageManager().sendMessage(sender, "player-not-found"));
            return null;
        });

        return true;
    }
}
