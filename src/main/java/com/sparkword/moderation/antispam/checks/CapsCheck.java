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
import com.sparkword.core.config.AntiSpamSettings;
import com.sparkword.moderation.antispam.SpamCheck;
import com.sparkword.moderation.antispam.SpamContext;
import com.sparkword.moderation.antispam.SpamManager;
import org.bukkit.entity.Player;

public class CapsCheck implements SpamCheck {
    private final SparkWord plugin;

    public CapsCheck(SparkWord plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpamManager.SpamResult check(Player player, SpamContext context) {
        if (!context.isChat()) return SpamManager.SpamResult.PASSED;

        AntiSpamSettings settings = plugin.getEnvironment().getConfigManager().getAntiSpamSettings();

        if (!settings.isCapsEnabled()) return SpamManager.SpamResult.PASSED;
        if (player.hasPermission("sparkword.bypass.caps")) return SpamManager.SpamResult.PASSED;

        String cleanMsg = context.cleanMessage();
        int capsLimit = settings.getCapsLimit();

        int capsCount = 0;
        for (char c : cleanMsg.toCharArray()) {
            if (Character.isUpperCase(c)) capsCount++;
        }

        if (capsCount > capsLimit) {
            String modifiedMessage = context.message().toLowerCase();
            return SpamManager.SpamResult.MODIFIED(modifiedMessage);
        }

        return SpamManager.SpamResult.PASSED;
    }
}
