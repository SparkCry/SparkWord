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
package com.sparkword.filters.word.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrieNode {

    private final Map<Character, TrieNode> children = new HashMap<>();
    private TrieNode fail;
    private final List<String> outputs = new ArrayList<>();

    public Map<Character, TrieNode> getChildren() { return children; }

    public TrieNode getFail() { return fail; }
    public void setFail(TrieNode fail) { this.fail = fail; }

    public List<String> getOutputs() { return outputs; }

    public void addOutput(String word) {
        if (!outputs.contains(word)) {
            outputs.add(word);
        }
    }
}
