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
package com.sparkword.spammers.checks;

import com.sparkword.SparkWord;
import com.sparkword.spammers.SpamCheck;
import com.sparkword.spammers.SpamContext;
import com.sparkword.spammers.SpamManager.SpamResult;
import org.bukkit.entity.Player;

public class CapsCheck implements SpamCheck {
    private final SparkWord plugin;

    public CapsCheck(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (!context.isChat()) return SpamResult.PASSED;

        if (!plugin.getConfig().getBoolean("anti-spam.caps-limit.enabled")) return SpamResult.PASSED;
        if (player.hasPermission("sparkword.bypass.caps")) return SpamResult.PASSED;

        String cleanMsg = context.cleanMessage();
        int capsLimit = plugin.getConfig().getInt("anti-spam.caps-limit.limit", 15);

        int capsCount = 0;
        for (char c : cleanMsg.toCharArray()) {
            if (Character.isUpperCase(c)) capsCount++;
        }

        if (capsCount > capsLimit) {
            String modifiedMessage = context.message().toLowerCase();
            return SpamResult.MODIFIED(modifiedMessage);
        }

        return SpamResult.PASSED;
    }
}
