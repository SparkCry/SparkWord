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
package com.sparkword.moderation.filters.util;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TextNormalizer {

    private static final Set<Integer> IGNORED_CODEPOINTS = Set.of(0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF, 0x180E);

    private static final char[] DIGIT_MAP = new char[]{'o', 'i', 'z', 'e', 'a', 's', 'g', 't', 'b', 'g'};

    private static final Map<Integer, Character> CONFUSABLES;

    static {
        Map<Integer, Character> map = new HashMap<>();
        map.put(0x0430, 'a');
        map.put(0x0410, 'a');
        map.put(0x0435, 'e');
        map.put(0x0415, 'e');
        map.put(0x043E, 'o');
        map.put(0x041E, 'o');
        map.put(0x0440, 'p');
        map.put(0x0420, 'p');
        map.put(0x0441, 'c');
        map.put(0x0421, 'c');
        map.put(0x0443, 'y');
        map.put(0x0423, 'y');
        map.put(0x0445, 'x');
        map.put(0x0425, 'x');
        map.put(0x0391, 'a');
        map.put(0x0392, 'b');
        map.put(0x0395, 'e');
        map.put(0x039F, 'o');
        map.put(0x03A1, 'p');
        CONFUSABLES = Collections.unmodifiableMap(map);
    }

    public static String sanitizeForDisplay(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); ) {
            int cp = input.codePointAt(i);
            int type = Character.getType(cp);
            if (type != Character.NON_SPACING_MARK && type != Character.COMBINING_SPACING_MARK) {
                sb.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private static boolean isSimpleLettersOnly(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    public static String normalizeForSearch(String input) {
        if (input == null || input.isEmpty()) return "";

        if (isSimpleLettersOnly(input)) {
            return input.toLowerCase();
        }

        StringBuilder sb = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); ) {
            int cp = input.codePointAt(i);

            int colorLen = getColorCodeLength(input, i);
            if (colorLen > 0) {
                i += colorLen;
                continue;
            }

            if (isIgnored(cp)) {
                i += Character.charCount(cp);
                continue;
            }

            char fastChar = getCanonicalCharFast(cp);
            if (fastChar != 0) {
                sb.append(fastChar);
            } else if (Character.isLetter(cp)) {
                appendComplexNormalization(sb, cp);
            }

            i += Character.charCount(cp);
        }

        String result = sb.toString();

        return hasExtendedChars(result) ? Normalizer.normalize(result, Normalizer.Form.NFKC) : result;
    }

    private static boolean hasExtendedChars(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 127) return true;
        }
        return false;
    }

    private static char getCanonicalCharFast(int codePoint) {
        if (Character.isDigit(codePoint)) {
            int val = Character.getNumericValue(codePoint);
            if (val >= 0 && val <= 9) {
                return DIGIT_MAP[val];
            }
        }
        return CONFUSABLES.getOrDefault(codePoint, (char) 0);
    }

    private static void appendComplexNormalization(StringBuilder sb, int codePoint) {
        if (codePoint <= 127) {
            sb.append(Character.toLowerCase((char) codePoint));
            return;
        }

        String source = new String(Character.toChars(codePoint));
        String decomposed = Normalizer.normalize(source, Normalizer.Form.NFKD);

        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                Character mapped = CONFUSABLES.get((int) c);
                if (mapped != null) {
                    sb.append(mapped);
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
    }

    public static CleanMapping buildCleanMapping(String original) {
        if (original == null) return new CleanMapping("", new int[0]);

        StringBuilder cleanSb = new StringBuilder(original.length());
        int[] indices = new int[original.length()];
        int cleanCount = 0;

        for (int i = 0; i < original.length(); ) {
            int cp = original.codePointAt(i);
            int charCount = Character.charCount(cp);

            int colorLen = getColorCodeLength(original, i);
            if (colorLen > 0) {
                i += colorLen;
                continue;
            }

            if (isIgnored(cp)) {
                i += charCount;
                continue;
            }

            char fastChar = getCanonicalCharFast(cp);

            if (fastChar != 0) {
                cleanSb.append(fastChar);
                if (cleanCount < indices.length) indices[cleanCount++] = i;
            } else if (Character.isLetter(cp)) {
                int startLen = cleanSb.length();
                appendComplexNormalization(cleanSb, cp);
                int added = cleanSb.length() - startLen;

                for (int k = 0; k < added; k++) {
                    if (cleanCount < indices.length) indices[cleanCount++] = i;
                }
            }

            i += charCount;
        }

        int[] finalIndices = new int[cleanCount];
        System.arraycopy(indices, 0, finalIndices, 0, cleanCount);
        return new CleanMapping(cleanSb.toString(), finalIndices);
    }

    public static boolean validateCharacters(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);

            if (IGNORED_CODEPOINTS.contains(cp)) return false;

            int type = Character.getType(cp);
            if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK) {
                return false;
            }

            if (Character.isLetter(cp)) {
                Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
                if (block != Character.UnicodeBlock.BASIC_LATIN && block != Character.UnicodeBlock.LATIN_1_SUPPLEMENT && block != Character.UnicodeBlock.LATIN_EXTENDED_A && block != Character.UnicodeBlock.LATIN_EXTENDED_B) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isIgnored(int cp) {
        return IGNORED_CODEPOINTS.contains(cp) || Character.getType(cp) == Character.FORMAT || Character.getType(cp) == Character.CONTROL;
    }

    private static int getColorCodeLength(String text, int index) {
        if (index + 1 >= text.length()) return 0;
        char c = text.charAt(index);

        if (c == '§' || c == '&') {
            char next = text.charAt(index + 1);

            if (next == '#') return (index + 7 < text.length()) ? 8 : 0;

            if (isColorChar(next)) {
                return 2;
            }
        }
        return 0;
    }

    private static boolean isColorChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= 'k' && c <= 'o') || c == 'r' || c == 'R' || c == 'x' || c == 'X';
    }

    public record CleanMapping(String cleanText, int[] originalIndices) {
    }

}
