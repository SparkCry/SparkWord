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

import com.sparkword.core.storage.model.LogEntry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ReportDAO {

    CompletableFuture<List<LogEntry>> getGlobalLogsStructAsync(String type, int page);

    CompletableFuture<List<String>> getPlayerScanReportAsync(int playerId, int page);

    CompletableFuture<List<String>> getPendingSuggestionsReportAsync(int page);
}
