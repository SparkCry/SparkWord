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
package com.sparkword.commands.impl.root;

import com.sparkword.commands.SubCommand;
import com.sparkword.core.Environment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Map;

public class InfoCommand implements SubCommand {
    private final Environment env;
    private final MiniMessage mm;

    public InfoCommand(Environment env) {
        this.env = env;
        this.mm = MiniMessage.miniMessage();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sparkword.info")) {
            env.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        if (page < 1 || page > 2) {
            env.getMessageManager().sendMessage(sender, "error-invalid-page");
            return true;
        }

        String v = env.getPlugin().getPluginMeta().getVersion();
        Component separator = mm.deserialize("<dark_gray><st>----------------------------------------");
        Component header = mm.deserialize(" <b><gradient:#09bbf5:#FFFFFF>SparkWord</gradient></b> <gray>v" + v + " <white>| <gray>Help (" + page + "/2)");

        sender.sendMessage(separator);
        sender.sendMessage(header);
        sender.sendMessage(separator);

        if (page == 1) {
            sendHelpLine(sender, "system.help.info");
            sendHelpLine(sender, "system.help.suggest");
            sendHelpLine(sender, "system.help.scan");
            sendHelpLine(sender, "system.help.checkmute");
            sendHelpLine(sender, "system.help.warn");
            sendHelpLine(sender, "system.help.mute");
            sendHelpLine(sender, "system.help.tempmute");
            sendHelpLine(sender, "system.help.unmute");
            sendHelpLine(sender, "system.help.permute");

            sender.sendMessage(Component.empty());
            sender.sendMessage(env.getMessageManager().getComponent("system.help.footer-next", Map.of("page", "2"), false));
        } else {
            sendHelpLine(sender, "system.help.list");
            sendHelpLine(sender, "system.help.add");
            sendHelpLine(sender, "system.help.remove");
            sendHelpLine(sender, "system.help.accept");
            sendHelpLine(sender, "system.help.deny");
            sendHelpLine(sender, "system.help.audit");
            sendHelpLine(sender, "system.help.logs");
            sendHelpLine(sender, "system.help.purge");
            sendHelpLine(sender, "system.help.reload");
            sendHelpLine(sender, "system.help.debug");
        }

        sender.sendMessage(separator);
        return true;
    }

    private void sendHelpLine(CommandSender sender, String key) {
        sender.sendMessage(env.getMessageManager().getComponent(key, Collections.emptyMap(), false));
    }
}
