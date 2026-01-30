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

public record AuditEntry(int id, String staffName, String action, String detail, long timestamp) {

    public String getFormattedDate() {
        return TimeUtil.formatShortDate(timestamp);
    }

    @Override
    public String toString() {
        return String.format("§7[%s] §e%s: §f%s §7- %s", getFormattedDate(), staffName, action, detail);
    }
}
