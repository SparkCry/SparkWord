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
package com.sparkword.moderation.filters.word;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum WordFilterMode {

    STRONG,

    NORMAL,

    WRITE_COMMAND;

    @Contract(pure = true)
    public @NotNull String getFileName() {
        return switch (this) {
            case STRONG -> "_Strong_.txt";
            case WRITE_COMMAND -> "_WriteCommand_.txt";
            default -> "_Normal_.txt";
        };
    }
}
