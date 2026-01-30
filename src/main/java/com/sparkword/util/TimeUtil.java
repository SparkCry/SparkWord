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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class TimeUtil {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault());

    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String formatShortDate(long millis) {
        return SHORT_DATE.format(Instant.ofEpochMilli(millis));
    }

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;

        long multiplier = 1;
        String timeStr = input.toLowerCase(Locale.ROOT);

        char suffix = timeStr.charAt(timeStr.length() - 1);
        if (Character.isLetter(suffix)) {
            timeStr = timeStr.substring(0, timeStr.length() - 1);
            switch (suffix) {
                case 'd' -> multiplier = 86400;
                case 'h' -> multiplier = 3600;
                case 'm' -> multiplier = 60;
                case 's' -> multiplier = 1;
                default -> {
                    return -1;
                }
            }
        }

        try {
            return Long.parseLong(timeStr) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";

        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 && days == 0 && hours == 0 && minutes == 0) sb.append(secs).append("s");

        return sb.toString().trim();
    }
}
