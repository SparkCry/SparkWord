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
package com.sparkword.core.storage.spi.dao;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerDAO {

    int getPlayerIdBlocking(UUID uuid, String name);

    CompletableFuture<Integer> getPlayerIdAsync(UUID uuid, String name);

    CompletableFuture<String> getPlayerNameAsync(int id);

    /**
     * Attempts to find a player ID by name from the database.
     * Useful for executing commands on offline players who are already registered.
     *
     * @param name The username to search for (case-insensitive).
     * @return A future containing the ID, or -1 if not found.
     */
    CompletableFuture<Integer> getPlayerIdByNameAsync(String name);
}
