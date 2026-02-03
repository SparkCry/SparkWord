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
package com.sparkword;

import com.sparkword.commands.CommandManager;
import com.sparkword.moderation.antispam.SpamManager;
import com.sparkword.moderation.filters.FilterManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SparkWord extends JavaPlugin {

    private static SparkWord instance;
    private Environment environment;

    private volatile boolean debugMode = false;
    private volatile boolean debugFilter = false;

    public static SparkWord getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        silenceLibraries();
        printBanner();

        try {

            if (!getDataFolder().exists()) getDataFolder().mkdirs();

            saveDefaultConfig();

            File modFile = new File(getDataFolder(), "moderation.yml");
            if (!modFile.exists()) saveResource("moderation.yml", false);

            File commandsFile = new File(getDataFolder(), "commands.yml");
            if (!commandsFile.exists()) saveResource("commands.yml", false);

            File localeFolder = new File(getDataFolder(), "locale");
            if (!localeFolder.exists()) localeFolder.mkdirs();

            File enLocale = new File(localeFolder, "en_US.yml");
            if (!enLocale.exists()) saveResource("locale/en_US.yml", false);

            this.debugMode = getConfig().getBoolean("debug", false);

            this.environment = new Environment(this);
            this.environment.load();

            long time = System.currentTimeMillis() - startTime;
            log("<white>Enabled successfully in <#09bbf5>" + time + "ms<white>.");

        } catch (Throwable t) {
            log("<red>CRITICAL ERROR starting SparkWord.");
            getLogger().log(Level.SEVERE, "", t);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (environment != null) {
            try {
                environment.shutdown();
            } catch (Exception e) {
                getLogger().warning("Error closing environment: " + e.getMessage());
            }
        }
        environment = null;
        instance = null;
        log("<white>Disabled successfully.");
    }

    public void reload() {
        try {
            reloadConfig();
            if (environment != null) {
                environment.reload();
            }
            debugMode = getConfig().getBoolean("debug", false);
        } catch (Exception e) {
            getLogger().severe("Error reloading SparkWord: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(
            "<#09bbf5>[SparkWord]</#09bbf5> <reset>" + message
                                                                                   ));
    }

    private void printBanner() {
        String[] banner = {
            "<#09bbf5> ___                 _    _ _ _               _   </#09bbf5>",
            "<#09bbf5>/ __> ___  ___  _ _ | |__| | | | ___  _ _  _| |  </#09bbf5>",
            "<#09bbf5>\\__ \\| . \\\\<_> || '_>| / /| | | |/ . \\| '_>/ . |  </#09bbf5>",
            "<#09bbf5><___/|  _/<___||_|  |_\\_\\|__/_/ \\___/|_|  \\___|  </#09bbf5>",
            "<#09bbf5>     |_|                                         </#09bbf5>"
        };

        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(line));
        }
    }

    private void silenceLibraries() {
        String[] packages = {
            "com.sparkword.libs.hikari",
            "com.sparkword.libs.hikari.HikariDataSource",
            "com.sparkword.libs.hikari.pool.HikariPool",
            "com.zaxxer.hikari"
        };

        try {
            Class<?> configurator = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> levelClazz = Class.forName("org.apache.logging.log4j.Level");
            Object levelWarn = levelClazz.getField("WARN").get(null);

            java.lang.reflect.Method setLevel = configurator.getMethod("setLevel", String.class, levelClazz);

            for (String pkg : packages) {
                setLevel.invoke(null, pkg, levelWarn);
            }
        } catch (Throwable ignored) {
            for (String pkg : packages) {
                Logger logger = Logger.getLogger(pkg);
                if (logger != null) logger.setLevel(Level.WARNING);
            }
        }
    }

    public Environment getEnvironment() {
        return environment;
    }

    public FilterManager getFilterManager() {
        return environment != null ? environment.getFilterManager() : null;
    }

    public SpamManager getSpamManager() {
        return environment != null ? environment.getSpamManager() : null;
    }

    public CommandManager getCommandManager() {
        return environment != null ? environment.getCommandManager() : null;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isDebugFilter() {
        return debugFilter;
    }

    public void setDebugFilter(boolean debugFilter) {
        this.debugFilter = debugFilter;
    }
}
