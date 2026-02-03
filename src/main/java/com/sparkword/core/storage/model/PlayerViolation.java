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
package com.sparkword.core.storage.model;

import com.sparkword.util.TimeUtil;
import org.jetbrains.annotations.NotNull;

public record PlayerViolation(int id, Type type, String reason, String staff, long timestamp, long expiry) {

    public @NotNull String toLogString() {
        String date = TimeUtil.formatShortDate(timestamp);
        String prefix = switch (type) {
            case WARN -> "§e[WARN]";
            case MUTE -> "§c[MUTE]";
        };

        return String.format("%s §7%s (%s): %s", prefix, date, staff, reason);
    }

    public enum Type {
        WARN,
        MUTE,
    }
}
