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

package teacommontea.veritesauver;

import teacommontea.veritesauver.util.SauverFormat;

import java.util.List;
import java.util.Locale;

public final class SauverDuration {

    public static final long SECOND = 1000L;
    public static final long MINUTE = 60L * SECOND;
    public static final long HOUR   = 60L * MINUTE;
    public static final long DAY    = 24L * HOUR;
    public static final long WEEK   = 7L * DAY;
    public static final long MONTH  = 30L * DAY;
    public static final long YEAR   = 365L * DAY;

    public static final List<String> SUGGESTIONS = List.of(
        "perm", "30m", "1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d", "1mo", "1y");

    private SauverDuration() {}

    public static long parse(String token) {
        if (token == null) {
            return -1;
        }
        String s = token.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return -1;
        }
        if (s.equals("perm") || s.equals("permanent") || s.equals("forever")) {
            return Entry.PERMANENT;
        }
        int i = 0;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
            i++;
        }
        if (i == 0 || i == s.length()) {
            return -1;
        }
        double n;
        try {
            n = Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return -1;
        }
        long unit = unitMillis(s.substring(i));
        return unit < 0 ? -1 : (long) (n * unit);
    }

    public static long parseTwoToken(String number, String unitWord) {
        double n;
        try {
            n = Double.parseDouble(number.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
        long unit = unitMillis(unitWord.trim().toLowerCase(Locale.ROOT));
        return unit < 0 ? -1 : (long) (n * unit);
    }

    private static long unitMillis(String unit) {
        return switch (unit) {
            case "s", "sec", "secs", "second", "seconds" -> SECOND;
            case "m", "min", "mins", "minute", "minutes" -> MINUTE;
            case "h", "hr", "hrs", "hour", "hours"       -> HOUR;
            case "d", "day", "days"                      -> DAY;
            case "w", "wk", "wks", "week", "weeks"       -> WEEK;
            case "mo", "month", "months"                 -> MONTH;
            case "y", "yr", "yrs", "year", "years"       -> YEAR;
            default -> -1L;
        };
    }

    public static String format(long millis) {
        return SauverFormat.fancyTime(millis);
    }
}
