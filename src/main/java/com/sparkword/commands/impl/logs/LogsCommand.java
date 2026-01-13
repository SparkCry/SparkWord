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

import com.sparkword.commands.SubCommand;
import com.sparkword.core.Environment;
import com.sparkword.model.LogEntry;
import com.sparkword.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public class LogsCommand implements SubCommand {
    private final Environment env;

    public LogsCommand(Environment env) { this.env = env; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.logs")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        String type = (args.length > 0) ? args[0].toLowerCase() : "b";
        int page = 1;
        if (args.length > 1) {
            try { page = Integer.parseInt(args[1]); } catch(Exception ignored){}
        }

        final int fPage = page;

        env.getStorage().getReports().getGlobalLogsStructAsync(type, page).thenAccept(logs -> {
            Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<dark_gray>--- <#09bbf5>Logs (" + type.toUpperCase() + ") Pg: " + fPage + "</#09bbf5> <dark_gray>---"
                ));

                if (logs.isEmpty()) {
                    env.getMessageManager().sendMessage(sender, "logs.viewer.no-records");
                    return;
                }

                for (LogEntry log : logs) {
                    sender.sendMessage(buildLogComponent(log));
                }
            });
        });
        return true;
    }

    private Component buildLogComponent(LogEntry log) {
        String dateStr = TimeUtil.formatShortDate(log.timestamp());
        String source = log.source() != null ? log.source() : "Unknown";
        String violation = log.violation();

        Component base = Component.text("[" + dateStr + "] ", NamedTextColor.GRAY)
            .append(Component.text(log.player() + " ", NamedTextColor.RED))
            .append(Component.text("| ", NamedTextColor.WHITE).append(Component.text(source + " ", NamedTextColor.GRAY)))
            .append(Component.text("| ", NamedTextColor.WHITE).append(Component.text(violation + " ", NamedTextColor.RED).append(Component.text("| ", NamedTextColor.WHITE))));

        Component hoverContent = buildHoverContent(log);

        return base.append(
            Component.text("[Content]")
                .color(NamedTextColor.WHITE)
                .hoverEvent(HoverEvent.showText(hoverContent))
        );
    }

    private Component buildHoverContent(LogEntry log) {
        String source = log.source().toLowerCase();
        String content = log.content();
        String detected = log.detectedWord();
        String violation = log.violation().toLowerCase();

        if (content == null) return Component.text("No content");

        if (source.contains("book")) {
            Component bookTitle = env.getMessageManager().getComponent("logs.viewer.hover-source-book");
            Component contentTitle = env.getMessageManager().getComponent("logs.viewer.hover-content-book");
            Component censorTitle = env.getMessageManager().getComponent("logs.viewer.hover-censor-book", Map.of("detected", detected != null ? detected : "?"), false);

            return bookTitle
                .append(Component.newline())
                .append(contentTitle)
                .append(Component.newline())
                .append(Component.text((content.length() > 600 ? content.substring(0, 600) + "..." : content), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(censorTitle);
        }

        if (source.contains("chat") || violation.contains("flood")) {
            if (violation.contains("flood")) {
                String[] msgs = content.split(Pattern.quote(" | "));
                Component floodTitle = env.getMessageManager().getComponent("logs.viewer.hover-flood");
                Component floodHover = floodTitle;
                for (String msg : msgs) {
                    floodHover = floodHover.append(Component.newline()).append(Component.text("- " + msg, NamedTextColor.GRAY));
                }
                return floodHover;
            }
            return formatChatContext(content, detected);
        }

        if (source.contains("sign") && !violation.contains("ip split")) {
            String[] lines = content.split(Pattern.quote(" | "));
            Component signHover = Component.text("Sign:", NamedTextColor.GRAY);
            for (String line : lines) {
                signHover = signHover.append(Component.newline())
                    .append(Component.text("- " + line, NamedTextColor.WHITE));
            }
            return signHover;
        }

        return Component.text("Detected Content:", NamedTextColor.GRAY)
            .append(Component.newline())
            .append(Component.text(content, NamedTextColor.RED));
    }

    private Component formatChatContext(String content, String detected) {
        if (detected == null || detected.isEmpty()) return Component.text(content, NamedTextColor.RED);

        String lowerContent = content.toLowerCase();
        String lowerDetected = detected.toLowerCase();
        int index = lowerContent.indexOf(lowerDetected);

        if (index == -1) return Component.text(content, NamedTextColor.RED);

        String[] words = content.split("\\s+");
        int targetIndex = -1;

        for(int i=0; i<words.length; i++) {
            if (words[i].toLowerCase().contains(lowerDetected)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) return Component.text(content, NamedTextColor.RED);

        int start = Math.max(0, targetIndex - 10);
        int end = Math.min(words.length, targetIndex + 11);

        Component builder = Component.text("...", NamedTextColor.GRAY);
        for (int i = start; i < end; i++) {
            if (i == targetIndex) {
                builder = builder.append(Component.text(words[i], NamedTextColor.RED)).append(Component.space());
            } else {
                builder = builder.append(Component.text(words[i], NamedTextColor.GRAY)).append(Component.space());
            }
        }
        builder = builder.append(Component.text("...", NamedTextColor.GRAY));
        return builder;
    }
}
