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
package com.sparkword.moderation.listeners;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sparkword.SparkWord;
import com.sparkword.core.storage.model.MuteInfo;
import com.sparkword.core.storage.model.MuteInfo.MuteScope;
import com.sparkword.moderation.antispam.SpamManager.SpamResult;
import com.sparkword.moderation.filters.util.TextNormalizer;
import com.sparkword.moderation.filters.word.result.FilterResult;
import com.sparkword.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BookListener implements Listener {
    private static final long MIN_EDIT_INTERVAL = 1500;
    private final SparkWord plugin;
    private final Cache<UUID, Long> messageCooldown;
    private final Cache<UUID, Long> muteNotifyCooldown;
    private final Cache<UUID, Long> staffNotifyCooldown;
    private final Cache<UUID, Long> bookOpenCooldown;
    private final Map<UUID, Long> bookEditCooldown = new ConcurrentHashMap<>();
    private final AtomicInteger globalEditsPerSecond = new AtomicInteger(0);

    public BookListener(SparkWord plugin) {
        this.plugin = plugin;
        this.messageCooldown = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(2)).build();
        this.muteNotifyCooldown = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(5)).build();
        this.staffNotifyCooldown = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(2)).build();
        this.bookOpenCooldown = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(2)).build();

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
            () -> globalEditsPerSecond.set(0), 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBookOpen(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isBook(item)) return;

        Player player = event.getPlayer();
        var env = plugin.getEnvironment();

        int pid = env.getPlayerDataManager().getPlayerId(player.getUniqueId(), player.getName());
        MuteInfo muteInfo = env.getPlayerDataManager().getMuteInfo(pid);

        if (muteInfo.blocks(MuteScope.GLOBAL)) {
            event.setCancelled(true);
            if (muteNotifyCooldown.getIfPresent(player.getUniqueId()) == null) {
                muteNotifyCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                env.getMessageManager().sendMessage(player, "moderation.player-permuted");
            }
            return;
        }

        int delaySeconds = plugin.getEnvironment().getConfigManager().getBookOpenDelay();
        if (delaySeconds <= 0) return;

        Long lastOpen = bookOpenCooldown.getIfPresent(player.getUniqueId());
        if (lastOpen != null) {
            event.setCancelled(true);
            if (messageCooldown.getIfPresent(player.getUniqueId()) == null) {
                messageCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                plugin.getEnvironment().getMessageManager().sendMessage(player, "book.limit-open",
                    Map.of("time", String.valueOf(delaySeconds)));
            }
        } else {
            bookOpenCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (delaySeconds * 1000L));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        Player player = event.getPlayer();

        if (event.getPreviousBookMeta().equals(event.getNewBookMeta())) {
            return;
        }

        if (plugin.getEnvironment().getConfigManager().isUnicodeEnabled()) {
            BookMeta newMeta = event.getNewBookMeta();
            boolean detectedZalgo = false;
            List<String> evidencePagesZalgo = new ArrayList<>();

            for (Component page : newMeta.pages()) {
                String plain = PlainTextComponentSerializer.plainText().serialize(page);
                evidencePagesZalgo.add(plain);
                if (!TextNormalizer.validateCharacters(plain)) {
                    detectedZalgo = true;
                }
            }

            if (detectedZalgo) {
                event.setCancelled(true);

                if (checkUserNotifyCooldown(player)) {
                    plugin.getEnvironment().getMessageManager().sendMessage(player, "notification.blocked", Map.of("reason", "Zalgo/Unicode"));
                }

                if (checkStaffNotifyCooldown(player)) {
                    plugin.getEnvironment().getNotifyManager().notifyBookBlocked(
                        player, "Zalgo/Unicode", "Corrupted Text", "Content Hidden (See Evidence)",
                        createEvidenceBook(player, evidencePagesZalgo)
                                                                                );
                }
                return;
            }
        }

        int maxGlobalRate = plugin.getEnvironment().getConfigManager().getBookGlobalRateLimit();
        if (maxGlobalRate > 0 && globalEditsPerSecond.incrementAndGet() > maxGlobalRate) {
            event.setCancelled(true);
            return;
        }

        long now = System.currentTimeMillis();

        if (now - bookEditCooldown.getOrDefault(player.getUniqueId(), 0L) < MIN_EDIT_INTERVAL) {
            event.setCancelled(true);
            return;
        }
        bookEditCooldown.put(player.getUniqueId(), now);

        if (!plugin.getEnvironment().getConfigManager().isFilterBooks()) return;

        BookMeta meta = event.getNewBookMeta();
        List<Component> originalPages = meta.pages();
        if (originalPages.isEmpty()) return;

        int maxPages = plugin.getEnvironment().getConfigManager().getBookMaxPages();

        if (originalPages.size() > maxPages) {
            event.setCancelled(true);
            plugin.getEnvironment().getMessageManager().sendMessage(player, "book.limit-pages");
            return;
        }

        int totalChars = 0;
        final int HARD_LIMIT = 8000;

        for (Component page : originalPages) {
            totalChars += PlainTextComponentSerializer.plainText().serialize(page).length();
            if (totalChars > HARD_LIMIT) {
                event.setCancelled(true);
                plugin.getEnvironment().getMessageManager().sendMessage(player, "book.limit-complexity");
                return;
            }
        }

        var env = plugin.getEnvironment();
        int pid = env.getPlayerDataManager().getPlayerId(player.getUniqueId(), player.getName());
        MuteInfo muteInfo = env.getPlayerDataManager().getMuteInfo(pid);

        if (muteInfo.blocks(MuteScope.GLOBAL)) {
            event.setCancelled(true);
            return;
        }

        if (player.hasPermission("sparkword.bypass.books")) return;

        List<Component> newPages = new ArrayList<>();
        List<String> evidencePages = new ArrayList<>();
        boolean contentModified = false;
        boolean notifyStaff = false;
        String triggerReason = "Book Filter";
        String detectedWord = "";
        String violatingPageContent = "";

        int maxChars = env.getConfigManager().getBookMaxPageChars();
        String replacement = env.getConfigManager().getGlobalReplacement();
        boolean replacementEnabled = env.getConfigManager().isReplacementEnabled();

        Component replacementComp = MiniMessage.miniMessage().deserialize(replacement);

        for (Component pageComp : originalPages) {
            String pageText = PlainTextComponentSerializer.plainText().serialize(pageComp);

            if (pageText.length() > maxChars) {
                pageText = pageText.substring(0, maxChars);
                contentModified = true;
            }

            evidencePages.add(pageText);

            SpamResult spam = env.getSpamManager().checkSpam(player, pageText, "Book", true, null, -1, false);

            if (spam.blocked()) {
                String rKey = spam.reasonKey().toLowerCase();
                boolean isSecurity = rKey.contains("ip") || rKey.contains("domain");

                if (isSecurity) {
                    event.setCancelled(true);
                    triggerReason = "IP/Security";
                    detectedWord = "Dangerous Pattern";
                    violatingPageContent = pageText;
                    notifyStaff = true;
                    break;
                }
            }

            FilterResult result = env.getFilterManager().processText(pageText, true, player);
            if (result.blocked()) {
                if (replacementEnabled) {
                    newPages.add(replacementComp);
                    contentModified = true;
                } else {
                    event.setCancelled(true);
                    if (checkUserNotifyCooldown(player)) {
                        env.getMessageManager().sendMessage(player, "notification.blocked", Map.of("reason", result.reason()));
                    }
                    break;
                }
            } else if (!result.detectedWords().isEmpty()) {
                String detected = result.detectedWords().iterator().next();

                if (!StringUtil.containsIgnoreCase(pageText, detected)) {
                    event.setCancelled(true);
                    if (checkUserNotifyCooldown(player)) {
                        env.getMessageManager().sendMessage(player, "notification.blocked", Map.of("reason", "Evasion"));
                    }
                    return;
                }

                if (replacementEnabled && result.processedMessage() != null && !result.processedMessage().equals(pageText)) {
                    newPages.add(Component.text(result.processedMessage()));
                    contentModified = true;
                } else {
                    newPages.add(Component.text(pageText));
                }
            } else {
                newPages.add(Component.text(pageText));
            }
        }

        if (!event.isCancelled() && contentModified) {
            meta.pages(newPages);
            event.setNewBookMeta(meta);
        }

        if (notifyStaff) {
            if (checkUserNotifyCooldown(player)) {
                env.getMessageManager().sendMessage(player, "notification.blocked", Map.of("reason", triggerReason));
            }
            if (checkStaffNotifyCooldown(player)) {
                plugin.getEnvironment().getNotifyManager().notifyBookBlocked(player, triggerReason, detectedWord, violatingPageContent, createEvidenceBook(player, evidencePages));
            }
        }
    }

    private boolean checkUserNotifyCooldown(Player p) {
        if (messageCooldown.getIfPresent(p.getUniqueId()) == null) {
            messageCooldown.put(p.getUniqueId(), System.currentTimeMillis());
            return true;
        }
        return false;
    }

    private boolean checkStaffNotifyCooldown(Player p) {
        if (staffNotifyCooldown.getIfPresent(p.getUniqueId()) == null) {
            staffNotifyCooldown.put(p.getUniqueId(), System.currentTimeMillis());
            return true;
        }
        return false;
    }

    private boolean isBook(ItemStack item) {
        return item != null && (item.getType() == Material.WRITABLE_BOOK || item.getType() == Material.WRITTEN_BOOK);
    }

    private ItemStack createEvidenceBook(Player player, List<String> pages) {
        ItemStack evidenceBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta evidenceMeta = (BookMeta) evidenceBook.getItemMeta();

        Component title = plugin.getEnvironment().getMessageManager().getComponent("book.evidence-title", Map.of("player", player.getName()), false);
        evidenceMeta.title(title);

        Component author = plugin.getEnvironment().getMessageManager().getComponent("book.author-name", Collections.emptyMap(), false);
        evidenceMeta.author(author);

        List<Component> comps = new ArrayList<>();
        int limit = 0;
        for (String p : pages) {
            if (limit++ > 10) break;
            comps.add(Component.text(p));
        }
        evidenceMeta.pages(comps);
        evidenceBook.setItemMeta(evidenceMeta);
        return evidenceBook;
    }
}
