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
package com.sparkword.moderation.antispam.checks;

import com.sparkword.SparkWord;
import com.sparkword.moderation.antispam.SpamCheck;
import com.sparkword.moderation.antispam.SpamContext;
import com.sparkword.moderation.antispam.SpamManager.SpamResult;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPCheck implements SpamCheck {

    private static final Pattern IP_PATTERN = Pattern.compile(
        "(?<![0-9])((?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))(?![0-9])"
                                                             );
    private final SparkWord plugin;

    public IPCheck(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (player.hasPermission("sparkword.bypass.ip")) return SpamResult.PASSED;

        String message = context.cleanMessage();
        Matcher matcher = IP_PATTERN.matcher(message);

        if (matcher.find()) {

            plugin.getEnvironment().getNotifyManager().notifyStaff(
                player,
                context.source(),
                "Anti-Ip",
                message,
                matcher.group(1)
                                                                  );
            return SpamResult.BLOCKED_WITH_REASON("spam.ip", false);
        }

        return SpamResult.PASSED;
    }

    public void cleanupPlayer(UUID uuid) {
    }
}
