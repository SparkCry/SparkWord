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
package com.sparkword.core.storage.spi;

import com.sparkword.core.ConfigManager;
import com.sparkword.core.storage.spi.dao.*;

import java.util.concurrent.Executor;

public interface StorageProvider {

    /**
     * Initialize the storage provider (connections, pools, etc.)
     *
     * @param config ConfigManager instance to read settings from
     */
    void init(ConfigManager config);

    /**
     * Close all connections and resources.
     */
    void shutdown();

    /**
     * Get the async executor for DB operations.
     */
    Executor getAsyncExecutor();

    // DAO Accessors
    PlayerDAO getPlayerDAO();

    MuteDAO getMuteDAO();

    WarningDAO getWarningDAO();

    MonitorDAO getMonitorDAO();

    AuditDAO getAuditDAO();

    SuggestionDAO getSuggestionDAO();

    ReportDAO getReportDAO();
}
