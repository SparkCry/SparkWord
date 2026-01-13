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
package com.sparkword.util;

import com.sparkword.SparkWord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker implements Listener {

    private final SparkWord plugin;
    private static final String GITHUB_REPO = "SparkCry/SparkWord";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private boolean updateAvailable = false;
    private String latestVersion = "";

    public UpdateChecker(SparkWord plugin) {
        this.plugin = plugin;
    }

    public void check() {
        if (!plugin.getEnvironment().getConfigManager().isUpdateCheck()) {
            return;
        }

        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "SparkWord")
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() == 200) {
                    processResponse(response.body());
                } else if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Update check failed. HTTP Code: " + response.statusCode());
                }
            })
            .exceptionally(e -> {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Update check error: " + e.getMessage());
                }
                return null;
            });
    }

    private void processResponse(String json) {
        Matcher matcher = VERSION_PATTERN.matcher(json);
        if (matcher.find()) {
            String rawVersion = matcher.group(1);
            String cleanRemoteVersion = rawVersion.startsWith("v") ? rawVersion.substring(1) : rawVersion;
            String currentVersion = plugin.getPluginMeta().getVersion();

            String cleanCurrent = currentVersion.split("-")[0];

            if (isNewerThan(cleanRemoteVersion, cleanCurrent)) {
                this.updateAvailable = true;
                this.latestVersion = cleanRemoteVersion;

                plugin.log("<yellow>New version available: <green>v" + cleanRemoteVersion);
                plugin.log("<gray>Current: v" + currentVersion);
                plugin.log("<yellow>Download at: <white>https://github.com/" + GITHUB_REPO + "/releases");
            }
        }
    }

    private boolean isNewerThan(String remote, String current) {
        try {
            String[] remoteParts = remote.split("\\.");
            String[] currentParts = current.split("\\.");
            int length = Math.max(remoteParts.length, currentParts.length);

            for (int i = 0; i < length; i++) {
                int r = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
                int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

                if (r > c) return true;
                if (r < c) return false;
            }
        } catch (NumberFormatException ignored) {
            return !remote.equalsIgnoreCase(current);
        }
        return false;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getEnvironment().getConfigManager().isUpdateCheck()) return;

        if (updateAvailable && event.getPlayer().hasPermission("sparkword.admin")) {
            event.getPlayer().sendMessage(Component.text()
                .append(Component.text("SparkWord ", NamedTextColor.GOLD))
                .append(Component.text("v" + latestVersion + " is available! ", NamedTextColor.GREEN))
                .append(Component.text("[Download]", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.openUrl("https://github.com/" + GITHUB_REPO + "/releases/latest")))
                .build()
            );
        }
    }
}
