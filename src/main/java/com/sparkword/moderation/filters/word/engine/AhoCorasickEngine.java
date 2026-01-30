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
package com.sparkword.moderation.filters.word.engine;

import java.util.*;

public class AhoCorasickEngine {

    private final TrieNode root;

    private AhoCorasickEngine(TrieNode root) {
        this.root = root;
    }

    public static AhoCorasickEngine fromWords(Collection<String> words) {
        TrieNode root = new TrieNode();

        for (String word : words) {
            if (word == null || word.isEmpty()) continue;

            String normalizedKey = word.toLowerCase(Locale.ROOT);

            TrieNode node = root;
            for (char c : normalizedKey.toCharArray()) {
                node = node.getChildren().computeIfAbsent(c, k -> new TrieNode());
            }
            node.addOutput(normalizedKey);
        }

        Queue<TrieNode> queue = new LinkedList<>();

        for (TrieNode child : root.getChildren().values()) {
            child.setFail(root);
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();

            for (Map.Entry<Character, TrieNode> entry : current.getChildren().entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();

                TrieNode fail = current.getFail();
                while (fail != null && !fail.getChildren().containsKey(c)) {
                    fail = fail.getFail();
                }

                if (fail != null) {
                    child.setFail(fail.getChildren().get(c));

                    child.getOutputs().addAll(child.getFail().getOutputs());
                } else {
                    child.setFail(root);
                }

                queue.add(child);
            }
        }

        return new AhoCorasickEngine(root);
    }

    public List<Match> findMatches(String text) {
        List<Match> matches = new ArrayList<>();
        TrieNode node = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            while (node != root && !node.getChildren().containsKey(c)) {
                node = node.getFail();
            }

            if (node.getChildren().containsKey(c)) {
                node = node.getChildren().get(c);
            }

            for (String output : node.getOutputs()) {

                matches.add(new Match(output, i - output.length() + 1, i));
            }
        }
        return matches;
    }

    public record Match(String word, int start, int end) {
    }
}
