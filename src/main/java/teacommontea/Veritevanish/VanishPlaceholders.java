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
import org.bukkit.OfflinePlayer;

import teacommontea.veritevanish.api.VanishAPI;

public final class VanishPlaceholders {

    private VanishPlaceholders() {}

    static void registerIfAvailable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new Expansion().register();
        } catch (Throwable ignored) {

        }
    }

    private static final class Expansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
        @Override public String getIdentifier() { return "verite"; }
        @Override public String getAuthor() { return "teacommontea"; }
        @Override public String getVersion() { return "1.0.0"; }
        @Override public boolean persist() { return true; }

        @Override
        public String onRequest(OfflinePlayer player, String params) {
            switch (params.toLowerCase()) {
                case "vanished":
                    return player != null && VanishAPI.isVanished(player.getUniqueId()) ? "true" : "false";
                case "vanish_count":
                    return String.valueOf(VanishAPI.getVanished().size());
                default:
                    return null;
            }
        }
    }
}
