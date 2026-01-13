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
package com.sparkword.util;

public final class StringUtil {

    private StringUtil() { throw new UnsupportedOperationException("Utility class"); }

    public static boolean containsIgnoreCase(String src, String what) {
        final int length = what.length();
        if (length == 0) return true;

        final char firstLo = Character.toLowerCase(what.charAt(0));
        final char firstUp = Character.toUpperCase(what.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            final char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp) continue;

            if (src.regionMatches(true, i, what, 0, length)) {
                return true;
            }
        }

        return false;
    }

    public static double similarity(CharSequence s1, CharSequence s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 100.0;

        int[] mtp = matches(s1, s2);
        float m = mtp[0];
        if (m == 0) return 0.0;

        float j = ((m / s1.length() + m / s2.length() + (m - mtp[1]) / m)) / 3;
        float jw = j < 0.7f ? j : j + Math.min(0.1f, 1f / mtp[3]) * mtp[2] * (1 - j);

        return jw * 100.0;
    }

    private static int[] matches(CharSequence s1, CharSequence s2) {
        CharSequence max, min;
        if (s1.length() > s2.length()) {
            max = s1; min = s2;
        } else {
            max = s2; min = s1;
        }

        int range = Math.max(max.length() / 2 - 1, 0);
        int[] matchIndexes = new int[min.length()];
        java.util.Arrays.fill(matchIndexes, -1);

        boolean[] matchFlags = new boolean[max.length()];
        int matches = 0;

        for (int i = 0; i < min.length(); i++) {
            char c1 = min.charAt(i);
            for (int xi = Math.max(i - range, 0), xn = Math.min(i + range + 1, max.length()); xi < xn; xi++) {
                if (!matchFlags[xi] && c1 == max.charAt(xi)) {
                    matchIndexes[i] = xi;
                    matchFlags[xi] = true;
                    matches++;
                    break;
                }
            }
        }

        char[] ms1 = new char[matches];
        char[] ms2 = new char[matches];

        for (int i = 0, si = 0; i < min.length(); i++) {
            if (matchIndexes[i] != -1) {
                ms1[si] = min.charAt(i);
                si++;
            }
        }

        for (int i = 0, si = 0; i < max.length(); i++) {
            if (matchFlags[i]) {
                ms2[si] = max.charAt(i);
                si++;
            }
        }

        int transpositions = 0;
        for (int i = 0; i < ms1.length; i++) {
            if (ms1[i] != ms2[i]) transpositions++;
        }

        int prefix = 0;
        for (int i = 0; i < min.length(); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }

        return new int[]{matches, transpositions / 2, prefix, max.length()};
    }
}
