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
package com.sparkword.simulation;

import com.sparkword.Environment;
import com.sparkword.SparkWord;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.MessageManager;
import com.sparkword.core.NotifyManager;
import com.sparkword.core.config.FilterSettings;
import com.sparkword.core.config.NotificationSettings;
import com.sparkword.core.storage.PlayerDataManager;
import com.sparkword.core.storage.model.MuteInfo;
import com.sparkword.moderation.antispam.SpamManager;
import com.sparkword.moderation.antispam.SpamManager.SpamResult;
import com.sparkword.moderation.filters.FilterManager;
import com.sparkword.moderation.filters.word.WordFilterMode;
import com.sparkword.moderation.filters.word.loader.WordListLoader;
import com.sparkword.moderation.listeners.ChatListener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PlayerChatSimulationTest {

    @Mock
    private SparkWord plugin;
    @Mock
    private Environment environment;
    @Mock
    private ConfigManager configManager;
    @Mock
    private PlayerDataManager playerDataManager;
    @Mock
    private NotifyManager notifyManager;
    @Mock
    private MessageManager messageManager;
    @Mock
    private SpamManager spamManager;
    @Mock
    private WordListLoader wordLoader;
    @Mock
    private Player player;
    @Mock
    private AsyncChatEvent chatEvent;

    @Mock
    private FilterSettings filterSettings;
    @Mock
    private NotificationSettings notificationSettings;

    private FilterManager filterManager;
    private ChatListener chatListener;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        when(plugin.getEnvironment()).thenReturn(environment);
        when(plugin.getLogger()).thenReturn(Logger.getGlobal());
        when(environment.getConfigManager()).thenReturn(configManager);
        when(environment.getPlayerDataManager()).thenReturn(playerDataManager);
        when(environment.getNotifyManager()).thenReturn(notifyManager);
        when(environment.getMessageManager()).thenReturn(messageManager);
        when(environment.getSpamManager()).thenReturn(spamManager);

        Set<String> normalWords = new HashSet<>();
        normalWords.add("vaca");

        when(wordLoader.loadWords(eq(WordFilterMode.NORMAL))).thenReturn(normalWords);
        when(wordLoader.loadWords(eq(WordFilterMode.STRONG))).thenReturn(Collections.emptySet());
        when(wordLoader.loadWords(eq(WordFilterMode.WRITE_COMMAND))).thenReturn(Collections.emptySet());

        when(configManager.getFilterSettings()).thenReturn(filterSettings);
        when(configManager.getNotificationSettings()).thenReturn(notificationSettings);

        when(configManager.isFilterChat()).thenReturn(true);
        when(configManager.isUnicodeEnabled()).thenReturn(true);
        when(configManager.isReplacementEnabled()).thenReturn(false);
        when(configManager.getGlobalReplacement()).thenReturn("****");
        when(configManager.isNotifyIconEnabled()).thenReturn(true);

        filterManager = new FilterManager(plugin, wordLoader);
        filterManager.loadFilters().join();

        when(environment.getFilterManager()).thenReturn(filterManager);

        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("TestPlayer");
        when(player.hasPermission(anyString())).thenReturn(false);

        when(chatEvent.getPlayer()).thenReturn(player);
        Set<Audience> viewers = mock(Set.class);
        when(chatEvent.viewers()).thenReturn(viewers);
        when(chatEvent.message()).thenReturn(Component.text(""));

        when(playerDataManager.getPlayerId(any(), any())).thenReturn(1);
        when(playerDataManager.getMuteInfo(1)).thenReturn(MuteInfo.NOT_MUTED);

        when(spamManager.checkSpam(any(), anyString(), anyString(), anyBoolean(), any(), anyInt(), anyBoolean()))
            .thenReturn(SpamResult.PASSED);

        chatListener = new ChatListener(plugin);
    }

    @Test
    @DisplayName("Simulation: 'v4c4' blocked (Must cancel event)")
    void testLeetspeakEvasionInGame() {
        String dirtyMessage = "Hola v4c4";
        when(chatEvent.message()).thenReturn(Component.text(dirtyMessage));

        chatListener.onPlayerChat(chatEvent);

        verify(chatEvent).setCancelled(true);

        verify(notifyManager).notifyStaff(eq(player), contains("Chat"), eq(dirtyMessage), eq("vaca"));
    }

    @Test
    @DisplayName("Simulation: 'vácá' blocked (Must cancel event)")
    void testAccentEvasionInGame() {
        String dirtyMessage = "That vácá is big";
        when(chatEvent.message()).thenReturn(Component.text(dirtyMessage));

        chatListener.onPlayerChat(chatEvent);

        verify(chatEvent).setCancelled(true);

        verify(notifyManager).notifyStaff(eq(player), contains("Chat"), eq(dirtyMessage), eq("vaca"));
    }
}
