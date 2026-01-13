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

public class DigitsLimitCheck implements SpamCheck {
    private final SparkWord plugin;

    public DigitsLimitCheck(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpamResult check(Player player, SpamContext context) {
        if (player.hasPermission("sparkword.bypass.digits")) return SpamResult.PASSED;

        int limit = context.isWritable()
            ? plugin.getEnvironment().getConfigManager().getDigitsLimitWritable()
            : plugin.getEnvironment().getConfigManager().getDigitsLimitChat();

        if (limit <= 0) return SpamResult.PASSED;

        int count = countVisibleDigits(context.cleanMessage());

        if (count > limit) {
            return SpamResult.BLOCKED_WITH_REASON("spam.digits", false);
        }
        return SpamResult.PASSED;
    }

    private int countVisibleDigits(String cleanText) {
        int count = 0;
        for (char c : cleanText.toCharArray()) {
            if (Character.isDigit(c)) count++;
        }
        return count;
    }
}
