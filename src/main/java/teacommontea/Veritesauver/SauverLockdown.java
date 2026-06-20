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

public final class SauverLockdown {

    private static volatile boolean active;
    private static volatile String reason;

    private SauverLockdown() {}

    public static boolean active() {
        return active;
    }

    public static String reason() {
        return reason == null ? "The server is locked down." : reason;
    }

    public static void begin(String why) {
        active = true;
        reason = why;
    }

    public static void end() {
        active = false;
        reason = null;
    }
}
