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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FilterListCommand implements SubCommand {
    private final Environment env;

    public FilterListCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.list")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        int page = 1;
        if (args.length > 1 && args[1].matches("\\d+")) page = Integer.parseInt(args[1]);

        if (args.length > 0 && (args[0].equals("n") || args[0].equals("s") || args[0].equals("wc"))) {
            WordFilterMode mode;
            if (args[0].equals("s")) mode = WordFilterMode.STRONG;
            else if (args[0].equals("wc")) mode = WordFilterMode.WRITE_COMMAND;
            else mode = WordFilterMode.NORMAL;

            Set<String> wordsSet = env.getFilterManager().getList(mode);

            if (wordsSet == null || wordsSet.isEmpty()) {
                env.getMessageManager().sendMessage(sender, "filter.list-empty");
                return true;
            }

            List<String> sortedWords = new ArrayList<>(wordsSet);
            sortedWords.sort(String::compareToIgnoreCase);

            int totalPages = (int) Math.ceil(sortedWords.size() / 20.0);
            if (page > totalPages) page = totalPages;
            int start = (page - 1) * 20;
            int end = Math.min(start + 20, sortedWords.size());

            String typeName = mode.name();

            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<dark_gray>--- <#09bbf5>List " + typeName + " (Page " + page + ")</#09bbf5> <dark_gray>---"
                                                                    ));

            sender.sendMessage(Component.text(String.join(", ", sortedWords.subList(start, end)), NamedTextColor.GRAY));
            return true;
        }

        if (args.length > 0 && args[0].equals("sg")) {
            final int fPage = page;

            env.getStorage().getPendingSuggestionsAsync(fPage).thenAccept(list -> {
                org.bukkit.Bukkit.getScheduler().runTask(env.getPlugin(), () -> {

                    Component suggestionsTitle = env.getMessageManager().getComponent("suggestions.pending-suggestion", Collections.emptyMap(), false);
                    String titleStr = MiniMessage.miniMessage().serialize(suggestionsTitle);

                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<dark_gray>--- <#09bbf5>" + titleStr + " (Page " + fPage + ")</#09bbf5> <dark_gray>---"
                                                                            ));

                    if (list.isEmpty()) env.getMessageManager().sendMessage(sender, "filter.list-empty");
                    else for (String str : list) sender.sendMessage(Component.text(str, NamedTextColor.YELLOW));
                });
            });
            return true;
        }

        env.getMessageManager().sendMessage(sender, "help.usage-list");
        return true;
    }
}
