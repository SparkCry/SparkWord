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
package com.sparkword.core.lifecycle;

import com.sparkword.SparkWord;
import com.sparkword.commands.CommandManager;
import com.sparkword.core.ClientBrandHandler;
import com.sparkword.core.ConfigManager;
import com.sparkword.moderation.listeners.*;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.event.EventPriority;

public class RegisterListener {

    private final SparkWord plugin;
    private final CommandManager commandManager;
    private ChatListener activeChatListener;

    public RegisterListener(SparkWord plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public void registerStaticListeners() {
        var pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new ClientBrandHandler(plugin), plugin);
        pm.registerEvents(new AnvilListener(plugin), plugin);
        pm.registerEvents(new BookListener(plugin), plugin);
        pm.registerEvents(new SignListener(plugin), plugin);
        pm.registerEvents(new CommandListener(plugin), plugin);
    }

    public void registerIncomingChannels() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", new ClientBrandHandler(plugin));
    }

    public void registerDynamicChatListener(ConfigManager configManager) {
        if (activeChatListener != null) {
            AsyncChatEvent.getHandlerList().unregister(activeChatListener);
        }

        EventPriority priority = configManager.getEventPriority();
        activeChatListener = new ChatListener(plugin);

        plugin.getServer().getPluginManager().registerEvent(AsyncChatEvent.class, activeChatListener, priority, (l, e) -> {
            if (e instanceof AsyncChatEvent ce) ((ChatListener) l).onPlayerChat(ce);
        }, plugin);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerBrigadierHints() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            if (commandManager != null) {
                commandManager.registerBrigadierTree(commands);
            }
        });
    }
}
