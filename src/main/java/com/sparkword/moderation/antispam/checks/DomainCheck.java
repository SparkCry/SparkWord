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
import com.sparkword.core.config.SecuritySettings;
import com.sparkword.moderation.antispam.SpamCheck;
import com.sparkword.moderation.antispam.SpamContext;
import com.sparkword.moderation.antispam.SpamManager.SpamResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.IDN;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainCheck implements SpamCheck {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]");
    private final SparkWord plugin;

    public DomainCheck(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (!plugin.getEnvironment().getConfigManager().isDomainEnabled()) return SpamResult.PASSED;

        SecuritySettings settings = plugin.getEnvironment().getConfigManager().getSecuritySettings();
        Pattern dotPattern = settings.getDotPattern();
        Pattern blacklistPattern = settings.getBlacklistPattern();

        String raw = context.cleanMessage();

        if (blacklistPattern != null) {
            String collapsed = NON_ALPHANUMERIC.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT);
            Matcher blMatcher = blacklistPattern.matcher(collapsed);

            if (blMatcher.find()) {
                String detected = blMatcher.group(1);
                plugin.getEnvironment().getNotifyManager().notifyStaff(
                    player, context.source(), "Anti-Domain (Blacklist)", context.cleanMessage(), "Detected: " + detected
                                                                      );
                return SpamResult.BLOCKED_WITH_REASON("spam.ip", false);
            }
        }

        if (dotPattern != null) {
            String normalized = dotPattern.matcher(raw).replaceAll(".");
            String collapsedDots = normalized.replaceAll("\\.+", ".");

            int len = collapsedDots.length();
            int dotIndex = collapsedDots.indexOf('.');

            while (dotIndex != -1) {
                if (dotIndex < len - 1) {

                    String tld = extractTldIgnoringSpaces(collapsedDots, dotIndex, 10, settings);

                    if (tld != null) {
                        String label = extractLabelPreceding(collapsedDots, dotIndex);

                        if (label != null && !label.isEmpty()) {
                            String candidate = label + "." + tld;

                            String canonical;
                            try {
                                String ascii = IDN.toASCII(candidate, IDN.ALLOW_UNASSIGNED);
                                canonical = ascii.toLowerCase(Locale.ROOT);
                                if (canonical.endsWith(".")) canonical = canonical.substring(0, canonical.length() - 1);
                            } catch (IllegalArgumentException e) {
                                dotIndex = collapsedDots.indexOf('.', dotIndex + 1);
                                continue;
                            }

                            if (settings.isWhitelisted(canonical)) {
                                dotIndex = collapsedDots.indexOf('.', dotIndex + 1);
                                continue;
                            }

                            plugin.getEnvironment().getNotifyManager().notifyStaff(
                                player, context.source(), "Anti-Domain", context.cleanMessage(), "Link: " + canonical
                                                                                  );
                            return SpamResult.BLOCKED_WITH_REASON("spam.ip", false);
                        }
                    }
                }
                dotIndex = collapsedDots.indexOf('.', dotIndex + 1);
            }
        }

        return SpamResult.PASSED;
    }

    private @Nullable String extractTldIgnoringSpaces(@NotNull String text, int dotIndex, int maxLen, SecuritySettings settings) {
        StringBuilder sb = new StringBuilder(8);
        int i = dotIndex + 1;
        int len = text.length();

        while (i < len && sb.length() < maxLen) {
            char c = text.charAt(i);

            if (Character.isWhitespace(c)) {
                String current = sb.toString();
                if (settings.isBlockedTLD(current)) {
                    return current;
                }
                i++;
                continue;
            }

            if (Character.isLetter(c)) {
                sb.append(Character.toLowerCase(c));
                i++;
                continue;
            }

            break;
        }

        String result = sb.toString();
        return settings.isBlockedTLD(result) ? result : null;
    }

    private @Nullable String extractLabelPreceding(String text, int dotIndex) {
        int endOfLabel = dotIndex;

        while (endOfLabel > 0 && Character.isWhitespace(text.charAt(endOfLabel - 1))) {
            endOfLabel--;
        }

        if (endOfLabel == 0) return null;

        int startOfLabel = endOfLabel;
        while (startOfLabel > 0) {
            char c = text.charAt(startOfLabel - 1);
            if (isValidLabelChar(c)) {
                startOfLabel--;
            } else {
                break;
            }
        }

        if (startOfLabel == endOfLabel) return null;

        return text.substring(startOfLabel, endOfLabel);
    }

    private boolean isValidLabelChar(char c) {
        return (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9') ||
            c == '-' ||
            c == '_';
    }
}
