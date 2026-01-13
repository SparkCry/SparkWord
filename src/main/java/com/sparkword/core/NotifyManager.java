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
package com.sparkword.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sparkword.SparkWord;
import com.sparkword.filters.util.TextNormalizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class NotifyManager {

    private final SparkWord plugin;
    private final Cache<String, ItemStack> evidenceBookCache;
    private final MiniMessage miniMessage;

    private static final String ICON_FLOOD = "🌊";
    private static final String ICON_IP = "🌐";
    private static final String ICON_ZALGO = "⚡";
    private static final String ICON_INJECTION = "💉";
    private static final String ICON_FILTER = "⚠";

    public NotifyManager(SparkWord plugin) {
        this.plugin = plugin;
        this.evidenceBookCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(100)
            .build();
        this.miniMessage = MiniMessage.miniMessage();
    }

    private boolean shouldNotify(String category) {
        if (category == null) return true;
        String c = category.toLowerCase();
        ConfigManager cfg = plugin.getEnvironment().getConfigManager();

        if (c.contains("repeat") || c.contains("digits") || c.contains("chars") || c.contains("caps")) return false;

        if (c.contains("flood") && !cfg.isNotifyTypeFlood()) return false;
        if ((c.contains("ip") || c.contains("domain")) && !cfg.isNotifyTypeIp()) return false;
        if ((c.contains("zalgo") || c.contains("unicode")) && !cfg.isNotifyTypeZalgo()) return false;
        if (c.contains("injection") && !cfg.isNotifyTypeInjection()) return false;

        if (!c.contains("flood") && !c.contains("ip") && !c.contains("zalgo") && !c.contains("injection")) {
            if (!cfg.isNotifyTypeFilter()) return false;
        }

        return true;
    }

    public void notifySuggestion(Player player, String word, String reason) {
        if (!hasStaffOnline()) return;

        Component msg = plugin.getEnvironment().getMessageManager().getComponent("notify-staff-suggest", Map.of(
            "player", player.getName(),
            "suggest", word,
            "reason", reason != null ? reason : "No reason"
        ), true);

        broadcastComponent(msg);
    }

    public void notifyEdit(Player player, String type, String censorType, String detectedWord, String fullContext) {
        notifyStaff(player, type, censorType, fullContext, detectedWord);
    }

    public void notifyStaff(Player offender, String category, String content, String detectedWord) {
        notifyStaff(offender, "Chat", category, content, detectedWord);
    }

    public void notifyStaff(Player offender, String source, String category, String content, String detectedWord) {
        if (source != null && source.equalsIgnoreCase("Book")) {
            return;
        }

        String safeCategory = (category != null) ? category : "General";
        String safeContent = (content != null) ? content : "";
        String safeSource = (source != null) ? source : "Chat";
        String safeDetected = (detectedWord != null) ? detectedWord : "Pattern";

        plugin.getEnvironment().getStorage().checkAndLog(offender.getName(), safeContent, safeCategory, safeSource, safeDetected);

        if (!shouldNotify(safeCategory) || !hasStaffOnline()) return;

        Component iconComponent = getIconComponent(safeSource, safeCategory, safeContent, safeDetected, null);

        Component message = plugin.getEnvironment().getMessageManager().getComponent(
            "alert",
            Map.of("player", offender.getName()),
            true
        ).append(iconComponent);

        broadcastComponent(message);
    }

    public void notifyInjection(Player player, String source, String content) {
        notifyStaff(player, source, "Injection", content, "Malicious Syntax");
    }

    public void notifyBookBlocked(Player player, String reason, String detectedWord, String pageContent, ItemStack originalBook) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        evidenceBookCache.put(id, originalBook);

        plugin.getEnvironment().getStorage().checkAndLog(player.getName(), pageContent, reason, "Book", detectedWord);

        if (!shouldNotify(reason) || !hasStaffOnline()) return;

        ClickEvent openBookEvent = ClickEvent.runCommand("/sw internal viewbook " + id);

        Component iconComponent = getIconComponent("Book", reason, "Book Content...", detectedWord, openBookEvent);

        Component message = plugin.getEnvironment().getMessageManager().getComponent(
            "alert",
            Map.of("player", player.getName()),
            true
        ).append(iconComponent);

        broadcastComponent(message);
    }

    public void notifySignBlocked(Player player, String reason, String detectedWord, String[] lines) {
        String flatContent = String.join(" | ", lines);
        notifyStaff(player, "Sign", reason, flatContent, detectedWord);
    }

    private Component getIconComponent(String source, String category, String content, String detected, ClickEvent clickEvent) {
        String iconSymbol = ICON_FILTER;
        NamedTextColor iconColor = NamedTextColor.YELLOW;

        String lowerCat = category.toLowerCase();

        if (lowerCat.contains("flood")) {
            iconSymbol = ICON_FLOOD;
            iconColor = NamedTextColor.AQUA;
        } else if (lowerCat.contains("ip") || lowerCat.contains("domain")) {
            iconSymbol = ICON_IP;
            iconColor = NamedTextColor.GREEN;
        } else if (lowerCat.contains("zalgo") || lowerCat.contains("unicode")) {
            iconSymbol = ICON_ZALGO;
            iconColor = NamedTextColor.LIGHT_PURPLE;
        } else if (lowerCat.contains("injection")) {
            iconSymbol = ICON_INJECTION;
            iconColor = NamedTextColor.DARK_RED;
        }

        MessageManager mm = plugin.getEnvironment().getMessageManager();
        String lowerSrc = source.toLowerCase();
        String displayContent = content;

        if (lowerSrc.contains("book")) {
            displayContent = "Here is the evidence (Click)";
        } else {
            displayContent = TextNormalizer.sanitizeForDisplay(content);
            if (displayContent.length() > 300) displayContent = displayContent.substring(0, 300) + "...";
        }

        Component hoverContent = mm.getComponent("notification.details.title", null, false)
            .append(Component.newline())
            .append(mm.getComponent("notification.details.type", Map.of("type", category), false))
            .append(Component.newline())
            .append(mm.getComponent("notification.details.source", Map.of("source", source), false))
            .append(Component.newline());

        if (lowerSrc.contains("book")) {
            hoverContent = hoverContent.append(mm.getComponent("notification.details.evidence-click", null, false).color(NamedTextColor.GOLD));
        } else {
            hoverContent = hoverContent.append(mm.getComponent("notification.details.detected", Map.of("detected", displayContent), false));
        }

        Component icon = Component.text("[" + iconSymbol + "]", iconColor)
            .hoverEvent(HoverEvent.showText(hoverContent));

        if (clickEvent != null) {
            icon = icon.clickEvent(clickEvent);
        }

        return icon;
    }

    public void openEvidenceBook(Player staff, String id) {
        ItemStack book = evidenceBookCache.getIfPresent(id);
        if (book != null) staff.openBook(book);
        else plugin.getEnvironment().getMessageManager().sendMessage(staff, "notification.details.evidence-error");
    }

    private boolean hasStaffOnline() { return !Bukkit.getOnlinePlayers().isEmpty(); }
    private void broadcastComponent(Component comp) { Bukkit.broadcast(comp, "sparkword.notify"); }
}
