/*
 * This file is part of Verite.
 * Copyright (C) 2026 teacommontea
 *
 * Verite is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * Verite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details. You should have received a copy of the license along with
 * Verite. If not, see <https://www.gnu.org/licenses/>.
 */

package teacommontea.veritesauver.util;

import java.util.Locale;

public final class SauverFormat {

    private static final long SECOND_MS = 1000L;
    private static final long MINUTE_MS = 60 * SECOND_MS;
    private static final long HOUR_MS = 60 * MINUTE_MS;
    private static final long DAY_MS = 24 * HOUR_MS;

    private SauverFormat() {}

    public static String fancyTime(long millis) {
        if (millis < 0) millis = 0;
        long days = millis / DAY_MS;
        long rem = millis % DAY_MS;
        long hours = rem / HOUR_MS;
        rem %= HOUR_MS;
        long minutes = rem / MINUTE_MS;
        rem %= MINUTE_MS;
        double seconds = rem / (double) SECOND_MS;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d");
            if (hours > 0) sb.append(" ").append(hours).append("h");
        } else if (hours > 0) {
            sb.append(hours).append("h");
            if (minutes > 0) sb.append(" ").append(minutes).append("m");
        } else {
            if (minutes > 0) {
                sb.append(minutes).append("m");
                long wholeSec = (long) seconds;
                if (wholeSec > 0) sb.append(" ").append(wholeSec).append("s");
            } else {
                if (seconds == Math.rint(seconds)) {
                    sb.append((long) seconds).append("s");
                } else {
                    sb.append(trimTrailingZero(String.format(Locale.ROOT, "%.1f", seconds))).append("s");
                }
            }
        }
        if (sb.length() == 0) sb.append("0s");
        return sb.toString();
    }

    public static String plural(long count, String noun) {
        return count + " " + pluralize(count, noun);
    }

    public static String pluralize(long count, String noun) {
        if (count == 1 || noun == null || noun.isEmpty()) {
            return noun;
        }
        String lower = noun.toLowerCase(Locale.ROOT);
        if (lower.endsWith("y") && noun.length() > 1 && !isVowel(lower.charAt(lower.length() - 2))) {
            return noun.substring(0, noun.length() - 1) + "ies";
        }
        if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
                || lower.endsWith("ch") || lower.endsWith("sh")) {
            return noun + "es";
        }
        return noun + "s";
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static String trimTrailingZero(String s) {
        if (s.contains(".")) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
