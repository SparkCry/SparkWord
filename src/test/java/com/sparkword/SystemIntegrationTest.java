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

import com.sparkword.commands.impl.filter.FilterAddCommand;
import com.sparkword.commands.impl.filter.FilterListCommand;
import com.sparkword.commands.impl.filter.FilterRemoveCommand;
import com.sparkword.commands.impl.logs.AuditCommand;
import com.sparkword.commands.impl.logs.LogsCommand;
import com.sparkword.commands.impl.logs.PurgeCommand;
import com.sparkword.commands.impl.logs.ScanCommand;
import com.sparkword.commands.impl.mute.*;
import com.sparkword.commands.impl.root.DebugCommand;
import com.sparkword.commands.impl.root.InfoCommand;
import com.sparkword.commands.impl.root.InternalCommand;
import com.sparkword.commands.impl.root.ReloadCommand;
import com.sparkword.commands.impl.suggest.AcceptCommand;
import com.sparkword.commands.impl.suggest.DenyCommand;
import com.sparkword.commands.impl.suggest.SuggestCommand;
import com.sparkword.core.ConfigManager;
import com.sparkword.core.MessageManager;
import com.sparkword.core.NotifyManager;
import com.sparkword.core.config.*;
import com.sparkword.core.storage.PlayerDataManager;
import com.sparkword.core.storage.StorageManager;
import com.sparkword.core.storage.model.AuditEntry;
import com.sparkword.core.storage.model.LogEntry;
import com.sparkword.core.storage.model.MuteInfo;
import com.sparkword.core.storage.model.MuteInfo.MuteScope;
import com.sparkword.core.storage.spi.dao.AuditDAO;
import com.sparkword.core.storage.spi.dao.ReportDAO;
import com.sparkword.core.storage.spi.dao.SuggestionDAO.SuggestionInfo;
import com.sparkword.moderation.antispam.SpamManager;
import com.sparkword.moderation.filters.FilterManager;
import com.sparkword.moderation.filters.word.loader.WordListLoader;
import com.sparkword.moderation.filters.word.result.FilterResult;
import com.sparkword.moderation.listeners.ChatListener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemIntegrationTest {

    @Mock
    private SparkWord plugin;
    @Mock
    private Environment env;
    @Mock
    private MessageManager messageManager;
    @Mock
    private ConfigManager configManager;
    @Mock
    private FilterManager filterManager;
    @Mock
    private WordListLoader wordLoader;
    @Mock
    private StorageManager storage;
    @Mock
    private NotifyManager notifyManager;
    @Mock
    private PlayerDataManager playerDataManager;
    @Mock
    private SpamManager spamManager;
    @Mock
    private FileConfiguration mockConfig;

    @Mock
    private GeneralSettings generalSettings;
    @Mock
    private FilterSettings filterSettings;
    @Mock
    private AntiSpamSettings antiSpamSettings;
    @Mock
    private SuggestionSettings suggestionSettings;
    @Mock
    private NotificationSettings notificationSettings;

    @Mock
    private CommandSender adminSender;
    @Mock
    private Player playerSender;
    @Mock
    private Server server;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private OfflinePlayer mockOfflinePlayer;
    @Mock
    private PlayerProfile mockPlayerProfile;

    @Mock
    private AuditDAO auditRepo;
    @Mock
    private ReportDAO reportRepo;

    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setup() {
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getServer).thenReturn(server);
        bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(playerSender));

        UUID fixedUUID = UUID.fromString("c0ddc939-5f0a-4a53-96c4-b558d6c5098e"); 
        lenient().when(mockOfflinePlayer.getUniqueId()).thenReturn(fixedUUID);
        lenient().when(mockOfflinePlayer.getName()).thenReturn("TargetPlayer");

        bukkitMock.when(() -> Bukkit.getPlayerExact(anyString())).thenReturn(null);

        bukkitMock.when(() -> Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(mockOfflinePlayer);

        bukkitMock.when(() -> Bukkit.getOfflinePlayer(anyString())).thenReturn(mockOfflinePlayer);
        bukkitMock.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(playerSender);

        lenient().when(server.createPlayerProfile(anyString())).thenReturn(mockPlayerProfile);

        lenient().doReturn(CompletableFuture.completedFuture(mockPlayerProfile)).when(mockPlayerProfile).update();

        lenient().when(mockPlayerProfile.getUniqueId()).thenReturn(fixedUUID);
        lenient().when(mockPlayerProfile.getName()).thenReturn("TargetPlayer");

        lenient().when(plugin.getEnvironment()).thenReturn(env);
        lenient().when(env.getPlugin()).thenReturn(plugin);
        lenient().when(plugin.getConfig()).thenReturn(mockConfig);
        lenient().when(env.getMessageManager()).thenReturn(messageManager);
        lenient().when(env.getConfigManager()).thenReturn(configManager);
        lenient().when(env.getFilterManager()).thenReturn(filterManager);
        lenient().when(env.getStorage()).thenReturn(storage);
        lenient().when(env.getNotifyManager()).thenReturn(notifyManager);
        lenient().when(env.getPlayerDataManager()).thenReturn(playerDataManager);
        lenient().when(env.getSpamManager()).thenReturn(spamManager);

        lenient().when(configManager.getGeneralSettings()).thenReturn(generalSettings);
        lenient().when(configManager.getFilterSettings()).thenReturn(filterSettings);
        lenient().when(configManager.getAntiSpamSettings()).thenReturn(antiSpamSettings);
        lenient().when(configManager.getSuggestionSettings()).thenReturn(suggestionSettings);
        lenient().when(configManager.getNotificationSettings()).thenReturn(notificationSettings);

        lenient().when(configManager.isFilterChat()).thenReturn(true);
        lenient().when(configManager.isSuggestionEnabled()).thenReturn(true);
        lenient().when(configManager.isUnicodeEnabled()).thenReturn(true);
        lenient().when(configManager.getSuggestionCooldown()).thenReturn(0);
        lenient().when(configManager.getSuggestionMaxWord()).thenReturn(20);
        lenient().when(configManager.getSuggestionMaxReason()).thenReturn(100);
        lenient().when(configManager.getEventPriority()).thenReturn(org.bukkit.event.EventPriority.HIGH);

        lenient().when(env.getAsyncExecutor()).thenReturn(Runnable::run);

        lenient().when(filterManager.getLoader()).thenReturn(wordLoader);
        lenient().when(storage.getAudit()).thenReturn(auditRepo);
        lenient().when(storage.getReports()).thenReturn(reportRepo);

        lenient().when(storage.getPlayerIdByNameAsync(anyString())).thenReturn(CompletableFuture.completedFuture(-1));
        lenient().when(storage.getPlayerIdAsync(any(UUID.class), anyString())).thenReturn(CompletableFuture.completedFuture(-1));

        lenient().when(messageManager.getString(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        lenient().when(mockConfig.contains(anyString())).thenReturn(false);

        lenient().when(adminSender.hasPermission(anyString())).thenReturn(true);
        lenient().when(adminSender.getName()).thenReturn("AdminUser");

        lenient().when(playerSender.hasPermission(anyString())).thenReturn(true);
        lenient().when(playerSender.getName()).thenReturn("TestPlayer");
        lenient().when(playerSender.getUniqueId()).thenReturn(UUID.randomUUID());

        lenient().when(scheduler.getMainThreadExecutor(any())).thenReturn(Runnable::run);

        lenient().when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(1)).run();
            return null;
        });

        lenient().when(scheduler.runTaskAsynchronously(any(Plugin.class), any(Runnable.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(1)).run();
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        if (bukkitMock != null) {
            bukkitMock.close();
        }
    }

    @Test
    @DisplayName("Command: /sw-mute (Permanent)")
    void testMuteCommand() {
        MuteCommand cmd = new MuteCommand(env);
        String targetName = "BadPlayer";

        when(storage.getPlayerIdAsync(any(UUID.class), anyString())).thenReturn(CompletableFuture.completedFuture(10));

        doAnswer(invocation -> {
            Consumer<Boolean> callback = invocation.getArgument(6);
            if (callback != null) callback.accept(true);
            return null;
        }).when(storage).mute(eq(10), anyString(), anyString(), anyLong(), eq("MUTE"), any(), any());

        cmd.execute(adminSender, new String[]{targetName, "Spamming"});

        verify(server).createPlayerProfile(eq(targetName));
        verify(mockPlayerProfile).update();

        verify(storage, timeout(100)).mute(eq(10), contains("Spamming"), eq("AdminUser"), eq(0L), eq("MUTE"), eq(MuteScope.CHAT), any());
        verify(messageManager, timeout(100)).sendMessage(eq(adminSender), eq("moderation.mute-success"), anyMap());
    }

    @Test
    @DisplayName("Command: /sw-tempmute")
    void testTempMuteCommand() {
        TempMuteCommand cmd = new TempMuteCommand(env);
        String targetName = "TempBadPlayer";

        when(storage.getPlayerIdAsync(any(UUID.class), anyString())).thenReturn(CompletableFuture.completedFuture(11));

        doAnswer(invocation -> {
            Consumer<Boolean> callback = invocation.getArgument(6);
            if (callback != null) callback.accept(true);
            return null;
        }).when(storage).mute(eq(11), anyString(), anyString(), anyLong(), eq("TEMPMUTE"), any(), any());

        cmd.execute(adminSender, new String[]{targetName, "1h", "Spam"});

        verify(server).createPlayerProfile(eq(targetName));
        verify(storage, timeout(100)).mute(eq(11), contains("Spam"), eq("AdminUser"), eq(3600L), eq("TEMPMUTE"), eq(MuteScope.CHAT), any());
    }

    @Test
    @DisplayName("Command: /sw-permute")
    void testPermuteCommand() {
        PermuteCommand cmd = new PermuteCommand(env);
        String targetName = "PermBanPlayer";

        when(storage.getPlayerIdAsync(any(), anyString())).thenReturn(CompletableFuture.completedFuture(55));

        cmd.execute(adminSender, new String[]{targetName, "Gross Misconduct"});

        verify(server).createPlayerProfile(eq(targetName));
        verify(storage, timeout(100)).mute(eq(55), contains("Gross Misconduct"), eq("AdminUser"), eq(0L), eq("PERMUTE"), eq(MuteScope.GLOBAL), any());
    }

    @Test
    @DisplayName("Command: /sw-unmute")
    void testUnmuteCommand() {
        UnmuteCommand cmd = new UnmuteCommand(env);
        String targetName = "ForgivenPlayer";

        when(storage.getPlayerIdAsync(any(), anyString())).thenReturn(CompletableFuture.completedFuture(12));
        when(storage.unmute(eq(12), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        cmd.execute(adminSender, new String[]{targetName});

        verify(server).createPlayerProfile(eq(targetName));
        verify(storage, timeout(100)).unmute(eq(12), eq("AdminUser"), contains("Manual"));
        verify(messageManager, timeout(100)).sendMessage(eq(adminSender), eq("moderation.unmute-success"), anyMap());
    }

    @Test
    @DisplayName("Command: /sw-checkmute")
    void testCheckMuteCommand() {
        CheckMuteCommand cmd = new CheckMuteCommand(env);
        String targetName = "CheckedPlayer";

        when(storage.getPlayerIdAsync(any(), anyString())).thenReturn(CompletableFuture.completedFuture(13));
        when(storage.getMuteTimeAsync(13)).thenReturn(CompletableFuture.completedFuture(0L));

        cmd.execute(adminSender, new String[]{targetName});

        verify(server).createPlayerProfile(eq(targetName));
        verify(storage).getMuteTimeAsync(13);
        verify(messageManager, timeout(100)).sendMessage(eq(adminSender), eq("moderation.check-mute-true"), anyMap());
    }

    @Test
    @DisplayName("Command: /sw-warn")
    void testWarnCommand() {
        WarnCommand cmd = new WarnCommand(env);

        bukkitMock.when(() -> Bukkit.getPlayer("InvalidPlayer")).thenReturn(null);
        when(storage.getPlayerIdAsync(any(UUID.class), anyString())).thenReturn(CompletableFuture.completedFuture(-1));

        cmd.execute(adminSender, new String[]{"InvalidPlayer", "Behave"});
        verify(messageManager, timeout(100)).sendMessage(eq(adminSender), eq("player-not-found"));
    }

    @Test
    @DisplayName("Command: /sw add")
    void testFilterAddCommand() {
        FilterAddCommand cmd = new FilterAddCommand(env);
        when(filterManager.addWordHotSwap(anyString(), any())).thenReturn(CompletableFuture.completedFuture(true));

        cmd.execute(adminSender, new String[]{"n", "badword"});

        verify(messageManager, never()).sendMessage(eq(adminSender), eq("no-permission"));
        verify(messageManager, timeout(100)).sendMessage(eq(adminSender), eq("filter.word-added"), anyMap());
    }

    @Test
    @DisplayName("Command: /sw remove")
    void testFilterRemoveCommand() {
        FilterRemoveCommand cmd = new FilterRemoveCommand(env);
        when(wordLoader.removeWordAsync(anyString(), any())).thenReturn(CompletableFuture.completedFuture(true));

        cmd.execute(adminSender, new String[]{"n", "badword"});

        verify(wordLoader).removeWordAsync(eq("badword"), any());
        verify(messageManager, timeout(100)).sendMessage(eq(adminSender), eq("filter.word-removed"), anyMap());
    }

    @Test
    @DisplayName("Command: /sw list (Standard)")
    void testFilterListCommand_Standard() {
        FilterListCommand cmd = new FilterListCommand(env);
        when(filterManager.getList(any())).thenReturn(Set.of("bad1", "bad2"));

        cmd.execute(adminSender, new String[]{"n"});

        verify(adminSender, atLeastOnce()).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("Command: /sw list (Suggestions)")
    void testFilterListCommand_Suggestions() {
        FilterListCommand cmd = new FilterListCommand(env);

        when(storage.getPendingSuggestionsAsync(1)).thenReturn(CompletableFuture.completedFuture(List.of("Suggestion 1")));
        when(messageManager.getComponent(eq("suggestions.pending-suggestion"), anyMap(), eq(false)))
            .thenReturn(Component.text("Suggestions"));

        cmd.execute(adminSender, new String[]{"sg"});

        verify(storage).getPendingSuggestionsAsync(1);
    }

    @Test
    @DisplayName("Command: /sw accept")
    void testAcceptCommand() {
        AcceptCommand cmd = new AcceptCommand(env);

        SuggestionInfo info = new SuggestionInfo("testword", UUID.randomUUID());
        doAnswer(invocation -> {
            Consumer<SuggestionInfo> callback = invocation.getArgument(3);
            callback.accept(info);
            return null;
        }).when(storage).acceptSuggestionAsync(eq(1), anyString(), eq("n"), any());

        when(filterManager.addWordHotSwap(anyString(), any())).thenReturn(CompletableFuture.completedFuture(true));

        cmd.execute(adminSender, new String[]{"1", "n"});

        verify(storage).acceptSuggestionAsync(eq(1), eq("AdminUser"), eq("n"), any());
        verify(messageManager, timeout(100)).sendMessage(eq(adminSender), eq("suggestions.action-suggest-accepted"), anyMap());
    }

    @Test
    @DisplayName("Command: /sw deny")
    void testDenyCommand() {
        DenyCommand cmd = new DenyCommand(env);

        doAnswer(invocation -> {
            Consumer<SuggestionInfo> callback = invocation.getArgument(2);
            callback.accept(new SuggestionInfo("bad", UUID.randomUUID()));
            return null;
        }).when(storage).denySuggestionAsync(eq(1), anyString(), any());

        cmd.execute(adminSender, new String[]{"1"});

        verify(storage).denySuggestionAsync(eq(1), eq("AdminUser"), any());
        verify(messageManager, timeout(100)).sendMessage(eq(adminSender), eq("suggestions.action-suggest-rejected"));
    }

    @Test
    @DisplayName("Command: /sw sg (Suggest)")
    void testSuggestCommand() {
        SuggestCommand cmd = new SuggestCommand(env);

        when(storage.getPlayerIdAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(1));
        when(storage.addSuggestionAsync(eq(1), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(true));

        cmd.execute(playerSender, new String[]{"badword", "It is bad"});

        verify(storage).addSuggestionAsync(eq(1), eq("badword"), eq("It is bad"));
        verify(messageManager, timeout(100)).sendMessage(eq(playerSender), eq("suggestions.suggest-success"));
    }

    @Test
    @DisplayName("Command: /sw audit")
    void testAuditCommand() {
        AuditCommand cmd = new AuditCommand(env);
        when(auditRepo.getAuditLogsStructAsync(isNull(), eq(10)))
            .thenReturn(CompletableFuture.completedFuture(List.of(new AuditEntry(1, "Staff", "MUTE", "Reason", System.currentTimeMillis()))));

        cmd.execute(adminSender, new String[]{});

        verify(auditRepo).getAuditLogsStructAsync(isNull(), eq(10));
        verify(adminSender, atLeastOnce()).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("Command: /sw logs")
    void testLogsCommand() {
        LogsCommand cmd = new LogsCommand(env);
        when(reportRepo.getGlobalLogsStructAsync(eq("b"), eq(1)))
            .thenReturn(CompletableFuture.completedFuture(List.of(new LogEntry("P1", "Chat", "Filter", "Bad", "bad", System.currentTimeMillis()))));

        cmd.execute(adminSender, new String[]{"b", "1"});

        verify(reportRepo).getGlobalLogsStructAsync(eq("b"), eq(1));
        verify(adminSender, atLeastOnce()).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("Command: /sw scan")
    void testScanCommand() {
        ScanCommand cmd = new ScanCommand(env);
        String target = "Suspect";

        when(storage.getPlayerIdAsync(any(), anyString())).thenReturn(CompletableFuture.completedFuture(50));

        when(storage.getPlayerReportAsync(eq(50), eq(1)))
            .thenReturn(CompletableFuture.completedFuture(List.of("Warn 1", "Mute 1")));

        cmd.execute(adminSender, new String[]{target});

        verify(server).createPlayerProfile(eq(target));
        verify(storage).getPlayerIdAsync(any(), anyString());
        verify(adminSender, atLeastOnce()).sendMessage(any(String.class));
    }

    @Test
    @DisplayName("Command: /sw purge")
    void testPurgeCommand() {
        PurgeCommand cmd = new PurgeCommand(env);

        cmd.execute(adminSender, new String[]{"sg", "30"});

        verify(messageManager).sendMessage(eq(adminSender), eq("moderation.action-purge-start"));
        verify(storage, timeout(100)).purgeData(eq("sg"), eq(30L));
    }

    @Test
    @DisplayName("Command: /sw-info")
    void testInfoCommand() {
        PluginDescriptionFile pdf = mock(PluginDescriptionFile.class);
        when(pdf.getVersion()).thenReturn("1.0-TEST");
        when(plugin.getPluginMeta()).thenReturn(pdf);

        InfoCommand cmd = new InfoCommand(env);

        cmd.execute(adminSender, new String[]{"2"});

        verify(adminSender, atLeastOnce()).sendMessage(any(Component.class));
        verify(messageManager).getComponent(eq("help.list"), anyMap(), eq(false));
    }

    @Test
    @DisplayName("Command: /sw-reload")
    void testReloadCommand() {
        ReloadCommand cmd = new ReloadCommand(env);
        cmd.execute(adminSender, new String[]{});

        verify(plugin).reload();
        verify(messageManager).sendMessage(eq(adminSender), eq("reload"));
    }

    @Test
    @DisplayName("Command: /sw-debug")
    void testDebugCommand() {
        when(plugin.isDebugMode()).thenReturn(false);

        DebugCommand cmd = new DebugCommand(env);
        cmd.execute(adminSender, new String[]{});

        verify(plugin).setDebugMode(true);
        verify(messageManager).sendMessage(eq(adminSender), eq("debug.general-enabled"));
    }

    @Test
    @DisplayName("Command: /sw internal viewbook")
    void testInternalCommand() {
        InternalCommand cmd = new InternalCommand(env);
        String bookId = "12345";

        cmd.execute(playerSender, new String[]{"viewbook", bookId});

        verify(notifyManager).openEvidenceBook(eq(playerSender), eq(bookId));
    }

    @Test
    @DisplayName("Listener: Chat Filter Blocking")
    void testChatListener_FilterBlock() {
        ChatListener listener = new ChatListener(plugin);

        when(configManager.isFilterChat()).thenReturn(true);

        when(playerDataManager.getPlayerId(any(), any())).thenReturn(100);
        when(playerDataManager.getMuteInfo(100)).thenReturn(MuteInfo.NOT_MUTED);

        when(playerSender.hasPermission("sparkword.bypass.chat")).thenReturn(false);

        FilterResult result = new FilterResult(true, null, "Illegal Word", "bad", Set.of("bad"), false);
        when(filterManager.processText(anyString(), eq(false), any())).thenReturn(result);

        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(playerSender);
        when(event.message()).thenReturn(Component.text("This is a bad message"));

        listener.onPlayerChat(event);

        verify(event).setCancelled(true);

        verify(notifyManager).notifyStaff(
            eq(playerSender),
            contains("Illegal Word"),
            anyString(),
            eq("bad")
        );

        verify(messageManager).sendMessage(eq(playerSender), eq("notification.blocked"), anyMap());
    }
}
