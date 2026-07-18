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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.Map;

final class VanishLevel {

    private static final String PREFIX = "verite.vanish.level.";

    private static Boolean lpPresent;

    private VanishLevel() {}

    static int tierOf(Player p) {
        if (p == null) return 0;
        if (!luckPermsPresent()) return 0;
        try {
            return readTier(p);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int readTier(Player p) {
        User user = LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId());
        if (user == null) return 0;

        Map<String, Boolean> perms = user.getCachedData()
                .getPermissionData(net.luckperms.api.query.QueryOptions.defaultContextualOptions())
                .getPermissionMap();
        int best = 0;
        for (Map.Entry<String, Boolean> e : perms.entrySet()) {
            if (!e.getValue()) continue;
            String key = e.getKey();
            if (key.startsWith(PREFIX)) {
                try {
                    best = Math.max(best, Integer.parseInt(key.substring(PREFIX.length())));
                } catch (NumberFormatException ignored) {

                }
            }
        }
        return best;
    }

    private static boolean luckPermsPresent() {
        Boolean cached = lpPresent;
        if (cached != null) return cached;
        boolean present = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        lpPresent = present;
        return present;
    }
}
