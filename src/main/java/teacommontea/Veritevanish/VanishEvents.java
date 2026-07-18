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

package teacommontea.veritevanish;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VanishEvents {

    public interface Listener {
        default void vanished(UUID player) {}
        default void unvanished(UUID player) {}
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private VanishEvents() {}

    public static void register(Listener l) {
        if (l != null) {
            LISTENERS.add(l);
        }
    }

    public static void unregister(Listener l) {
        LISTENERS.remove(l);
    }

    static void fireVanished(UUID player) {
        for (Listener l : LISTENERS) {
            try {
                l.vanished(player);
            } catch (Throwable t) {
                warn("vanished", t);
            }
        }
    }

    static void fireUnvanished(UUID player) {
        for (Listener l : LISTENERS) {
            try {
                l.unvanished(player);
            } catch (Throwable t) {
                warn("unvanished", t);
            }
        }
    }

    private static void warn(String which, Throwable t) {
        Vanish v = Vanish.instance();
        if (v != null && v.plugin() != null) {
            v.plugin().getLogger().warning("A vanish " + which + " listener threw: " + t);
        }
    }
}
