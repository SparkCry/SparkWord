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
import com.sparkword.core.storage.model.AuditEntry;
import com.sparkword.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuditCommand implements SubCommand {
    private final Environment env;

    public AuditCommand(Environment env) {
        this.env = env;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.audit")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        String target = (args.length > 0) ? args[0] : "Global";

        env.getStorage().getAudit().getAuditLogsStructAsync(args.length > 0 ? args[0] : null, 10).thenAccept(logs -> {
            Bukkit.getScheduler().runTask(env.getPlugin(), () -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<dark_gray>--- <#09bbf5>Audit Logs: " + target + "</#09bbf5> <dark_gray>---"
                                                                        ));

                if (logs.isEmpty()) {
                    env.getMessageManager().sendMessage(sender, "logs.viewer.no-records");
                    return;
                }

                for (AuditEntry log : logs) {
                    sender.sendMessage(formatEntry(log));
                }
            });
        });
        return true;
    }

    private Component formatEntry(AuditEntry log) {
        String dateStr = TimeUtil.formatShortDate(log.timestamp());
        String action = log.action();
        Map<String, String> data = parseDetail(log.detail());

        Component base = env.getMessageManager().getComponent("audit.format.base", Map.of(
            "date", dateStr,
            "staff", log.staffName()
                                                                                         ), false);

        Component details = switch (action.toUpperCase()) {
            case "MUTE" ->
                env.getMessageManager().getComponent("audit.format.actions.mute", Map.of("player", data.getOrDefault("Player", "?")), false)
                    .append(hoverReason(data.get("Reason")));

            case "PERMUTE" ->
                env.getMessageManager().getComponent("audit.format.actions.permute", Map.of("player", data.getOrDefault("Player", "?")), false)
                    .append(hoverReason(data.get("Reason")));

            case "TEMPMUTE" ->
                env.getMessageManager().getComponent("audit.format.actions.tempmute", Map.of("player", data.getOrDefault("Player", "?")), false)
                    .append(hoverReason(data.get("Reason")));

            case "UNMUTE" ->
                env.getMessageManager().getComponent("audit.format.actions.unmute", Map.of("player", data.getOrDefault("Player", "?")), false)
                    .append(hoverReason(data.get("Reason")));

            case "WARN" ->
                env.getMessageManager().getComponent("audit.format.actions.warn", Map.of("player", data.getOrDefault("Player", "?")), false)
                    .append(hoverReason(data.get("Reason")));

            case "PURGE" -> env.getMessageManager().getComponent("audit.format.actions.purge", Map.of(
                "type", data.getOrDefault("Type", "all"),
                "days", data.getOrDefault("Days", "0")
                                                                                                     ), false);

            case "ACCEPT" -> env.getMessageManager().getComponent("audit.format.actions.accept", Map.of(
                "word", data.getOrDefault("Word", "?"),
                "list", data.getOrDefault("List", "n")
                                                                                                       ), false);

            case "DENY" -> env.getMessageManager().getComponent("audit.format.actions.deny", Map.of(
                "word", data.getOrDefault("Word", "?")
                                                                                                   ), false);

            case "API_MUTE" ->
                env.getMessageManager().getComponent("audit.format.actions.api-mute", Map.of("player", data.getOrDefault("Player", "?")), false)
                    .append(hoverReason(data.get("Reason")));

            default -> env.getMessageManager().getComponent("audit.format.actions.default", Map.of(
                "action", action,
                "detail", log.detail()
                                                                                                  ), false);
        };

        return base.append(details);
    }

    private Component hoverReason(String reason) {
        String safeReason = reason != null ? reason : "No reason";
        Component hoverText = env.getMessageManager().getComponent("audit.hover-reason", Collections.emptyMap(), false);

        return hoverText.hoverEvent(HoverEvent.showText(Component.text(safeReason, NamedTextColor.GRAY)));
    }

    private Map<String, String> parseDetail(String detail) {
        Map<String, String> map = new HashMap<>();
        if (detail == null) return map;
        String[] parts = detail.split("\\|");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }
}
