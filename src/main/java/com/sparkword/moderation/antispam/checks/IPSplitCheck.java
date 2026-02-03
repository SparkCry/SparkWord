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
package com.sparkword.moderation.antispam.checks;

import com.sparkword.SparkWord;
import com.sparkword.moderation.antispam.SpamCheck;
import com.sparkword.moderation.antispam.SpamContext;
import com.sparkword.moderation.antispam.SpamManager.PunishmentType;
import com.sparkword.moderation.antispam.SpamManager.SpamResult;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPSplitCheck implements SpamCheck {

    private static final Pattern SPLIT_IP_PATTERN = Pattern.compile(
        "(?:^|[^0-9])((?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:[^0-9]+(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3})(?:$|[^0-9])"
                                                                   );
    private static final int MAX_HISTORY_ENTRIES = 5;
    private final SparkWord plugin;
    private final Map<UUID, LinkedList<HistoryEntry>> historyMap = new ConcurrentHashMap<>();

    public IPSplitCheck(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (!plugin.getEnvironment().getConfigManager().isIpSplitEnabled()) return SpamResult.PASSED;
        if (player.hasPermission("sparkword.bypass.ip.split")) return SpamResult.PASSED;

        String cleanContent = context.cleanMessage().trim();
        if (cleanContent.isEmpty() || cleanContent.length() > 50) return SpamResult.PASSED;

        if (!hasRelevantCharacters(cleanContent)) {
            return SpamResult.PASSED;
        }

        LinkedList<HistoryEntry> history = historyMap.computeIfAbsent(player.getUniqueId(), k -> new LinkedList<>());

        String combinedText;

        synchronized (history) {
            if (!history.isEmpty() && history.getFirst().text().equals(cleanContent)) {
                return SpamResult.PASSED;
            }

            history.addFirst(HistoryEntry.from(cleanContent, context.signLocation()));
            if (history.size() > MAX_HISTORY_ENTRIES) history.removeLast();

            StringBuilder sb = new StringBuilder(MAX_HISTORY_ENTRIES * 50);
            for (int i = history.size() - 1; i >= 0; i--) {
                sb.append(history.get(i).text()).append(" ");
            }
            combinedText = sb.toString();
        }

        String filteredText = filterRepeatedNumbers(combinedText);

        if (!hasEnoughDigits(filteredText)) return SpamResult.PASSED;

        if (filteredText.length() > 300) filteredText = filteredText.substring(0, 300);

        Matcher matcher = SPLIT_IP_PATTERN.matcher(filteredText);

        if (matcher.find()) {
            if (isCountingSequence(matcher.group(1))) return SpamResult.PASSED;

            final List<HistoryEntry> snapshot;
            synchronized (history) {
                snapshot = new ArrayList<>(history);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (HistoryEntry entry : snapshot) {
                    Location loc = entry.toLocation(plugin.getServer());
                    if (loc != null && loc.isWorldLoaded()) {
                        if (loc.getBlock().getState() instanceof Sign sign) {
                            for (int i = 0; i < 4; i++) {
                                sign.line(i, Component.empty());
                            }
                            sign.update(true, false);
                        }
                    }
                }
            });

            historyMap.remove(player.getUniqueId());

            plugin.getSpamManager().triggerAutoMute(
                player,
                "anti-spam.ip-split-detection.mute",
                "30m",
                "IP-Split",
                PunishmentType.PERMUTE
                                                   );

            plugin.getEnvironment().getNotifyManager().notifyStaff(
                player,
                context.source(),
                "IP Split",
                filteredText,
                "Pattern Detected"
                                                                  );

            return SpamResult.BLOCKED_WITH_REASON("spam.ip-split", false);
        }

        return SpamResult.PASSED;
    }

    private String filterRepeatedNumbers(String text) {
        String[] tokens = text.split("[^0-9]+");
        List<String> validNumbers = new ArrayList<>();
        String lastNum = null;
        int runCount = 0;

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            if (token.equals(lastNum)) {
                runCount++;
            } else {
                lastNum = token;
                runCount = 1;
            }

            if (runCount <= 2) {
                validNumbers.add(token);
            }
        }

        return String.join(" ", validNumbers);
    }

    private boolean hasRelevantCharacters(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c) || c == '.') return true;
        }
        return false;
    }

    private boolean hasEnoughDigits(String text) {
        int digits = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                digits++;
                if (digits >= 4) return true;
            }
        }
        return false;
    }

    private boolean isCountingSequence(String text) {
        try {
            List<Integer> numbers = Arrays.stream(text.split("[^0-9]+"))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();

            if (numbers.size() < 4) return false;
            int firstDiff = numbers.get(1) - numbers.get(0);
            if (Math.abs(firstDiff) > 10 || firstDiff == 0) return false;
            for (int i = 1; i < numbers.size() - 1; i++) {
                if (numbers.get(i + 1) - numbers.get(i) != firstDiff) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void clearHistory(UUID uuid) {
        historyMap.remove(uuid);
    }

    private record HistoryEntry(String text, String worldName, int x, int y, int z) {
        public static HistoryEntry from(String text, Location loc) {
            if (loc == null) return new HistoryEntry(text, null, 0, 0, 0);
            return new HistoryEntry(text, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        public Location toLocation(org.bukkit.Server server) {
            if (worldName == null) return null;
            org.bukkit.World world = server.getWorld(worldName);
            return (world != null) ? new Location(world, x, y, z) : null;
        }
    }
}
