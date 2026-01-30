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
package com.sparkword.commands.impl.filter;

import com.sparkword.Environment;
import com.sparkword.commands.SubCommand;
import com.sparkword.moderation.filters.word.WordFilterMode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class FilterRemoveCommand implements SubCommand {
    private final Environment env;

    public FilterRemoveCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.remove")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 1) {
            env.getMessageManager().sendMessage(sender, "help.usage-remove");
            return true;
        }

        String type = "n";
        int startIndex = 0;
        int endIndex = args.length;

        String firstArg = args[0].toLowerCase();
        if (isType(firstArg)) {
            type = firstArg;
            startIndex = 1;
        } else if (args.length > 1) {
            String lastArg = args[args.length - 1].toLowerCase();
            if (isType(lastArg)) {
                type = lastArg;
                endIndex = args.length - 1;
            }
        }

        if (startIndex >= endIndex) {
            env.getMessageManager().sendMessage(sender, "help.usage-remove");
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            sb.append(args[i]).append(" ");
        }
        String word = sb.toString().trim();

        WordFilterMode mode = getMode(type);
        final String listName = mode.name();

        env.getFilterManager().getLoader().removeWordAsync(word, mode).thenAccept(success -> {
            Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                if (success) {
                    env.getMessageManager().sendMessage(sender, "filter.word-removed", Map.of("list", listName));
                    env.getFilterManager().loadFilters();
                } else {
                    env.getMessageManager().sendMessage(sender, "filter.word-not-found");
                }
            });
        });

        return true;
    }

    private boolean isType(String arg) {
        return arg.equals("n") || arg.equals("s") || arg.equals("wc");
    }

    private WordFilterMode getMode(String type) {
        return switch (type) {
            case "s" -> WordFilterMode.STRONG;
            case "wc" -> WordFilterMode.WRITE_COMMAND;
            default -> WordFilterMode.NORMAL;
        };
    }
}
