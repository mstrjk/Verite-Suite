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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SauverEvents {

    public interface Listener {
        default void entryAdded(Entry entry) {}
        default void entryRemoved(Entry entry) {}
        default void broadcastSent(String message, String target) {}
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private SauverEvents() {}

    public static void register(Listener l) {
        if (l != null) {
            LISTENERS.add(l);
        }
    }

    public static void unregister(Listener l) {
        LISTENERS.remove(l);
    }

    static void fireAdded(Entry e) {
        for (Listener l : LISTENERS) {
            try {
                l.entryAdded(e);
            } catch (Throwable t) {
                warn("entryAdded", t);
            }
        }
    }

    static void fireRemoved(Entry e) {
        for (Listener l : LISTENERS) {
            try {
                l.entryRemoved(e);
            } catch (Throwable t) {
                warn("entryRemoved", t);
            }
        }
    }

    static void fireBroadcast(String message, String target) {
        for (Listener l : LISTENERS) {
            try {
                l.broadcastSent(message, target);
            } catch (Throwable t) {
                warn("broadcastSent", t);
            }
        }
    }

    private static void warn(String which, Throwable t) {
        Sauver s = Sauver.instance();
        if (s != null && s.plugin() != null) {
            s.plugin().getLogger().warning("A punishment " + which + " listener threw: " + t);
        }
    }
}
