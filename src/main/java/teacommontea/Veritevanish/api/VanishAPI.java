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

package teacommontea.veritevanish.api;

import org.bukkit.entity.Player;

import teacommontea.veritevanish.Vanish;
import teacommontea.veritevanish.VanishEvents;

import java.util.Set;
import java.util.UUID;

public final class VanishAPI {

    private VanishAPI() {}

    private static Vanish vanish() {
        return Vanish.instance();
    }

    public static boolean enabled() {
        return vanish() != null;
    }

    public static boolean isVanished(UUID player) {
        return enabled() && vanish().isVanished(player);
    }

    public static Set<UUID> getVanished() {
        return enabled() ? vanish().getVanished() : Set.of();
    }

    public static boolean vanish(Player p) {
        return enabled() && vanish().vanish(p);
    }

    public static boolean unvanish(Player p) {
        return enabled() && vanish().unvanish(p);
    }

    public static boolean toggle(Player p) {
        return enabled() && vanish().toggle(p);
    }

    public static void registerListener(VanishEvents.Listener listener) {
        VanishEvents.register(listener);
    }

    public static void unregisterListener(VanishEvents.Listener listener) {
        VanishEvents.unregister(listener);
    }
}
