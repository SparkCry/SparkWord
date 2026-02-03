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
package com.sparkword.core.storage.impl.sql;

import com.sparkword.core.storage.impl.sql.query.QueryAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class SchemaManager {

    private final SQLConnectionFactory pool;
    private final Logger logger;
    private final QueryAdapter queryAdapter;

    public SchemaManager(SQLConnectionFactory pool, Logger logger, QueryAdapter queryAdapter) {
        this.pool = pool;
        this.logger = logger;
        this.queryAdapter = queryAdapter;
    }

    public void runMigrations() {
        try (Connection conn = pool.getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute(queryAdapter.getTableCreationQuery("players"));
            stmt.execute(queryAdapter.getTableCreationQuery("suggestion"));
            stmt.execute(queryAdapter.getTableCreationQuery("warnings"));
            stmt.execute(queryAdapter.getTableCreationQuery("muted"));
            stmt.execute(queryAdapter.getTableCreationQuery("mute_history"));
            stmt.execute(queryAdapter.getTableCreationQuery("audit"));
            stmt.execute(queryAdapter.getTableCreationQuery("monitor_logs"));

            createIndices(stmt);

        } catch (SQLException e) {
            logger.severe("Error executing DB migrations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createIndices(Statement stmt) throws SQLException {
        try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON monitor_logs(timestamp)");
        } catch (SQLException ignored) {
        }

        try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_warnings_player ON warnings(player_id)");
        } catch (SQLException ignored) {
        }

        try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mute_history_player ON mute_history(player_id)");
        } catch (SQLException ignored) {
        }

        try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_uuid ON players(uuid)");
        } catch (SQLException ignored) {
        }
    }
}
