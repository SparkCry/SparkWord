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
package com.sparkword.core.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SecuritySettings {
    private final Set<String> domainWhitelist = new HashSet<>();
    private final Set<String> blockedTLDs = new HashSet<>();

    private Pattern dotPattern;
    private Pattern blacklistPattern;

    public void load(FileConfiguration config) {
        this.domainWhitelist.clear();
        if (config.contains("domain-filter.whitelist")) {
            List<String> list = config.getStringList("domain-filter.whitelist");
            for (String domain : list) {
                this.domainWhitelist.add(domain.toLowerCase());
            }
        }

        this.blockedTLDs.clear();
        if (config.contains("domain-filter.blocked-tlds")) {
            List<String> tlds = config.getStringList("domain-filter.blocked-tlds");
            for (String tld : tlds) {
                this.blockedTLDs.add(tld.toLowerCase());
            }
        }

        if (config.contains("domain-filter.blacklist")) {
            List<String> blacklist = config.getStringList("domain-filter.blacklist");
            if (!blacklist.isEmpty()) {
                String blacklistUnion = blacklist.stream()
                    .map(s -> s.replaceAll("[^a-zA-Z0-9]", ""))
                    .filter(s -> !s.isEmpty())
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));

                if (!blacklistUnion.isEmpty()) {
                    this.blacklistPattern = Pattern.compile("(" + blacklistUnion + ")", Pattern.CASE_INSENSITIVE);
                } else {
                    this.blacklistPattern = null;
                }
            } else {
                this.blacklistPattern = null;
            }
        } else {
            this.blacklistPattern = null;
        }

        List<String> dotList;
        if (config.contains("domain-filter.dot")) {
            dotList = config.getStringList("domain-filter.dot");
        } else {
            dotList = List.of("[\\p{P}\\p{S}]", "dot", "point");
        }

        if (!dotList.isEmpty()) {
            String dotUnion = dotList.stream()
                .map(s -> {
                    if (s.contains("\\")) {
                        return s;
                    } else {
                        String q = Pattern.quote(s);
                        return "(?:" +
                            "\\b" + q + "\\b|" +
                            "\\[\\s*" + q + "\\s*\\]|" +
                            "\\(\\s*" + q + "\\s*\\)|" +
                            "\\{\\s*" + q + "\\s*\\}|" +
                            "<\\s*" + q + "\\s*>)";
                    }
                })
                .collect(Collectors.joining("|"));

            this.dotPattern = Pattern.compile(dotUnion, Pattern.CASE_INSENSITIVE);
        } else {
            this.dotPattern = Pattern.compile("[\\p{P}\\p{S}]");
        }
    }

    public boolean isWhitelisted(String domain) {
        return domainWhitelist.contains(domain.toLowerCase());
    }

    public boolean isBlockedTLD(String tld) {
        return blockedTLDs.contains(tld.toLowerCase());
    }

    public Pattern getDotPattern() {
        return dotPattern;
    }

    public Pattern getBlacklistPattern() {
        return blacklistPattern;
    }
}
