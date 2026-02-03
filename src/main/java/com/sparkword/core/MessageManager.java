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

import com.sparkword.SparkWord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class MessageManager {

    private final SparkWord plugin;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;
    private YamlConfiguration messagesConfig;
    private String prefixString;

    public MessageManager(SparkWord plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
        reload();
    }

    public void reload() {
        String targetLocale = configManager.getGeneralSettings().getLocale();
        if (targetLocale == null || targetLocale.isEmpty()) targetLocale = "en_US";

        File localeFolder = new File(plugin.getDataFolder(), "locale");
        if (!localeFolder.exists()) localeFolder.mkdirs();

        File targetFile = new File(localeFolder, targetLocale + ".yml");

        if (!targetFile.exists()) {
            if (plugin.getResource("locale/" + targetLocale + ".yml") != null) {
                plugin.saveResource("locale/" + targetLocale + ".yml", false);
            } else {
                plugin.getLogger().warning("Locale file '" + targetLocale + ".yml' not found in JAR or Disk. Falling back to 'en_US.yml'.");
                targetLocale = "en_US";
                targetFile = new File(localeFolder, "en_US.yml");

                if (!targetFile.exists() && plugin.getResource("locale/en_US.yml") != null) {
                    plugin.saveResource("locale/en_US.yml", false);
                }
            }
        }

        if (targetFile.exists()) {
            messagesConfig = YamlConfiguration.loadConfiguration(targetFile);
        } else {
            messagesConfig = new YamlConfiguration();
            plugin.getLogger().severe("CRITICAL: Could not load any locale file.");
        }

        prefixString = messagesConfig.getString("prefix", "<#09bbf5>[SparkWord] <reset>");
    }

    public String getPrefix() {
        return prefixString;
    }

    public String getString(String key) {
        if (messagesConfig == null) return key;
        return messagesConfig.getString(key, key);
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Collections.emptyMap());
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        Component messageComp = getComponent(key, placeholders, true);
        if (messageComp != Component.empty()) {
            sender.sendMessage(messageComp);
        }
    }

    public void sendMessage(CommandSender sender, String key, TagResolver... customResolvers) {
        String rawMsg = messagesConfig.getString(key);
        if (rawMsg == null || rawMsg.isEmpty()) return;

        String combinedRaw = prefixString + rawMsg;
        Component messageComp = miniMessage.deserialize(combinedRaw, customResolvers);
        sender.sendMessage(messageComp);
    }

    public Component getComponent(String key, Map<String, String> placeholders, boolean usePrefix) {
        if (placeholders == null) placeholders = Collections.emptyMap();

        String rawMsg = messagesConfig.getString(key);
        if (rawMsg == null) return Component.text(key);

        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            Component val = miniMessage.deserialize(entry.getValue());
            resolvers.add(Placeholder.component(entry.getKey(), val));
        }

        String combined = usePrefix ? (prefixString + rawMsg) : rawMsg;

        for (String k : placeholders.keySet()) {
            combined = combined.replace("{" + k + "}", "<" + k + ">");
        }

        return miniMessage.deserialize(combined, TagResolver.resolver(resolvers));
    }

    public Component getComponent(String key) {
        return getComponent(key, Collections.emptyMap(), true);
    }

    public Component getSpyIconComponent(String originalText, Set<String> detectedWords, boolean replacementEnabled) {
        String iconSymbol = messagesConfig.getString("notification.icon-symbol", "⚠️");
        Component hoverContent;

        if (replacementEnabled) {
            String detectedList = detectedWords != null && !detectedWords.isEmpty()
                ? String.join(", ", detectedWords)
                : "N/A";

            hoverContent = getComponent("notification.spy-hover.matches", Map.of("matches", detectedList), false);
        } else {
            String safeOriginal = originalText.length() > 200 ? originalText.substring(0, 200) + "..." : originalText;
            hoverContent = getComponent("notification.spy-hover.original", Map.of("original", safeOriginal), false);
        }

        return Component.space()
            .append(Component.text(iconSymbol, NamedTextColor.YELLOW))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(hoverContent));
    }
}
