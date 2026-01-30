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
package com.sparkword.core.storage.impl.sql.provider;

import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.storage.impl.sql.SQLConnectionFactory;
import com.sparkword.core.storage.impl.sql.SchemaManager;
import com.sparkword.core.storage.impl.sql.dao.*;
import com.sparkword.core.storage.impl.sql.query.MySQLQueryAdapter;
import com.sparkword.core.storage.spi.StorageProvider;
import com.sparkword.core.storage.spi.dao.*;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySQLProvider implements StorageProvider {

    private final SparkWord plugin;
    private SQLConnectionFactory connectionFactory;
    private SchemaManager schemaManager;

    private ExecutorService writer;
    private ExecutorService reader;

    private PlayerDAO playerDAO;
    private MuteDAO muteDAO;
    private WarningDAO warningDAO;
    private MonitorDAO monitorDAO;
    private AuditDAO auditDAO;
    private SuggestionDAO suggestionDAO;
    private ReportDAO reportDAO;

    public MySQLProvider(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init(ConfigManager config) {
        this.connectionFactory = new SQLConnectionFactory(plugin);
        this.connectionFactory.init(config);

        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SparkWord-DB-Writer");
            t.setDaemon(false);
            return t;
        });
        this.reader = Executors.newVirtualThreadPerTaskExecutor();

        this.schemaManager = new SchemaManager(connectionFactory, plugin.getLogger(), new MySQLQueryAdapter());
        this.schemaManager.runMigrations();

        MySQLQueryAdapter adapter = new MySQLQueryAdapter();
        this.playerDAO = new SQLPlayerDAO(connectionFactory, writer, reader, adapter);
        this.muteDAO = new SQLMuteDAO(connectionFactory, writer, reader, adapter);
        this.warningDAO = new SQLWarningDAO(connectionFactory, writer, reader);
        this.monitorDAO = new SQLMonitorDAO(connectionFactory, writer, reader);
        this.auditDAO = new SQLAuditDAO(connectionFactory, writer, reader);
        this.suggestionDAO = new SQLSuggestionDAO(connectionFactory, writer, reader);
        this.reportDAO = new SQLReportDAO(connectionFactory, writer, reader);
    }

    @Override
    public void shutdown() {
        if (writer != null) writer.shutdown();
        if (connectionFactory != null) connectionFactory.close();
    }

    @Override
    public Executor getAsyncExecutor() {
        return writer;
    }

    @Override
    public PlayerDAO getPlayerDAO() {
        return playerDAO;
    }

    @Override
    public MuteDAO getMuteDAO() {
        return muteDAO;
    }

    @Override
    public WarningDAO getWarningDAO() {
        return warningDAO;
    }

    @Override
    public MonitorDAO getMonitorDAO() {
        return monitorDAO;
    }

    @Override
    public AuditDAO getAuditDAO() {
        return auditDAO;
    }

    @Override
    public SuggestionDAO getSuggestionDAO() {
        return suggestionDAO;
    }

    @Override
    public ReportDAO getReportDAO() {
        return reportDAO;
    }
}
