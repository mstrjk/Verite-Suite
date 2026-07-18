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

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

final class VanishSettings {

    boolean fakeMessage = true;
    boolean actionbar = true;
    boolean fly = true;
    boolean invulnerability = true;
    boolean effects = true;
    boolean gamemode = true;
    boolean rideEntity = true;
    boolean silentContainer = true;
    boolean inventoryInspect = true;
    boolean serverPing = true;
    boolean ghost = true;
    boolean preventChat = true;
    boolean preventPickup = true;
    boolean preventDrop = true;
    boolean preventInteract = true;
    boolean preventBlockBreak = true;
    boolean preventBlockPlace = true;
    boolean preventTarget = true;
    boolean preventDamage = true;
    boolean preventFood = true;
    boolean preventBuckets = true;
    boolean preventAdvancement = true;

    private VanishSettings() {}

    static VanishSettings load(Plugin plugin) {
        VanishSettings s = new VanishSettings();
        File f = new File(plugin.getDataFolder(), "settings.yml");
        if (!f.isFile()) {
            return s;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        s.fakeMessage        = y.getBoolean("vanish.features.fake_message", true);
        s.actionbar          = y.getBoolean("vanish.features.actionbar", true);
        s.fly                = y.getBoolean("vanish.features.fly", true);
        s.invulnerability    = y.getBoolean("vanish.features.invulnerability", true);
        s.effects            = y.getBoolean("vanish.features.effects", true);
        s.gamemode           = y.getBoolean("vanish.features.gamemode", true);
        s.rideEntity         = y.getBoolean("vanish.features.ride_entity", true);
        s.silentContainer    = y.getBoolean("vanish.features.silent_container", true);
        s.inventoryInspect   = y.getBoolean("vanish.features.inventory_inspect", true);
        s.serverPing         = y.getBoolean("vanish.features.server_ping", true);
        s.ghost              = y.getBoolean("vanish.features.ghost", true);
        s.preventChat        = y.getBoolean("vanish.features.prevent_chat", true);
        s.preventPickup      = y.getBoolean("vanish.features.prevent_pickup", true);
        s.preventDrop        = y.getBoolean("vanish.features.prevent_drop", true);
        s.preventInteract    = y.getBoolean("vanish.features.prevent_interact", true);
        s.preventBlockBreak  = y.getBoolean("vanish.features.prevent_block_break", true);
        s.preventBlockPlace  = y.getBoolean("vanish.features.prevent_block_place", true);
        s.preventTarget      = y.getBoolean("vanish.features.prevent_target", true);
        s.preventDamage      = y.getBoolean("vanish.features.prevent_damage", true);
        s.preventFood        = y.getBoolean("vanish.features.prevent_food", true);
        s.preventBuckets     = y.getBoolean("vanish.features.prevent_buckets", true);
        s.preventAdvancement = y.getBoolean("vanish.features.prevent_advancement", true);
        return s;
    }
}
