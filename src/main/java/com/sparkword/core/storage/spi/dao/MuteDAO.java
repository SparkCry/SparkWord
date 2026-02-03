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

import com.sparkword.core.storage.model.MuteInfo;
import com.sparkword.core.storage.model.MuteInfo.MuteScope;

import java.util.concurrent.CompletableFuture;

public interface MuteDAO {

    CompletableFuture<Void> muteAsync(int playerId, String reason, String by, long durationSeconds, MuteScope scope);

    CompletableFuture<Void> unmuteAsync(int playerId);

    MuteInfo fetchMuteInfoBlocking(int playerId);

    CompletableFuture<Long> getMuteExpiryAsync(int playerId);

    CompletableFuture<Integer> purgeHistoryAsync(long daysOld);
}
