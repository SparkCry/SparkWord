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
package com.sparkword.spammers.security;

import com.sparkword.SparkWord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InputSanitizer {

    private final List<SanitizationStage> stages;

    public InputSanitizer(SparkWord plugin) {
        this.stages = new ArrayList<>();

        this.stages.add(new UnicodeNormalizationStage());

        if (plugin.getEnvironment().getConfigManager().isAntiInjectionTags()) {
            this.stages.add(new MiniMessageSafeFilterStage());
        }

        if (plugin.getEnvironment().getConfigManager().isAntiInjectionPapi()) {
            this.stages.add(new PlaceholderEscapingStage());
        }
    }

    public String sanitize(String rawInput, Player source) {
        if (source.hasPermission("sparkword.bypass.injection")) return rawInput;

        String current = rawInput;
        for (SanitizationStage stage : stages) {
            current = stage.process(current, source);
        }
        return current;
    }

    private static class UnicodeNormalizationStage implements SanitizationStage {
        private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{C}&&[^\\n\\u00A7\\u200B]]");

        @Override
        public String process(String input, Player source) {
            if (input == null) return "";
            String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);
            return CONTROL_CHARS.matcher(normalized).replaceAll("");
        }
    }

    private static class MiniMessageSafeFilterStage implements SanitizationStage {
        private final MiniMessage safeSerializer;

        public MiniMessageSafeFilterStage() {
            TagResolver safeTags = TagResolver.builder()
                .resolver(StandardTags.color())
                .resolver(StandardTags.decorations())
                .resolver(StandardTags.rainbow())
                .resolver(StandardTags.gradient())
                .build();

            this.safeSerializer = MiniMessage.builder()
                .tags(safeTags)
                .strict(false)
                .build();
        }

        @Override
        public String process(String input, Player source) {
            try {
                Component safeComponent = safeSerializer.deserialize(input);
                return safeSerializer.serialize(safeComponent);
            } catch (Exception e) {
                return MiniMessage.miniMessage().escapeTags(input);
            }
        }
    }

    private static class PlaceholderEscapingStage implements SanitizationStage {
        @Override
        public String process(String input, Player source) {
            if (input.contains("%")) {
                return input.replace("%", "%\u00A0");
            }
            return input;
        }
    }
}
