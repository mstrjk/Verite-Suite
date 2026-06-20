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

import java.util.TreeMap;

public final class SauverProtocol {

    private static final TreeMap<Integer, String> TABLE = new TreeMap<>();

    static {

        TABLE.put(763, "1.20.1");
        TABLE.put(764, "1.20.2");
        TABLE.put(765, "1.20.4");
        TABLE.put(766, "1.20.6");

        TABLE.put(767, "1.21.1");
        TABLE.put(768, "1.21.3");
        TABLE.put(769, "1.21.4");
        TABLE.put(770, "1.21.5");
        TABLE.put(771, "1.21.6");
        TABLE.put(772, "1.21.7");
        TABLE.put(773, "1.21.8");
        TABLE.put(774, "1.21.9");
        TABLE.put(775, "1.21.10");
        TABLE.put(776, "1.21.11");

        TABLE.put(800, "26.1");
        TABLE.put(801, "26.1.1");
        TABLE.put(802, "26.1.2");
    }

    private SauverProtocol() {}

    public static String versionName(int protocol) {
        if (protocol <= 0) {
            return null;
        }
        var floor = TABLE.floorEntry(protocol);
        if (floor == null) {
            return "pre-1.20 (legacy)";
        }
        if (protocol > TABLE.lastKey()) {
            return floor.getValue() + "+";
        }
        return floor.getValue();
    }
}
