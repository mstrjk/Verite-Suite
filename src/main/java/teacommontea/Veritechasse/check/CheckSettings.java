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

package teacommontea.veritechasse.check;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CheckSettings {

    private static final Map<String, String> CATEGORY = new HashMap<>();
    static {
        cat("combat", "reach", "hitbox", "killaura", "criticals", "autoclicker", "autoblock",
                "velocity", "fastbow", "length");
        cat("movement", "speed", "motion", "sprint", "strafe", "step", "highjump", "fastclimb",
                "wallclimb", "antilevitation", "nofall", "noslow", "phase", "vclip", "elytrafly",
                "boatfly", "jesus", "flight");
        cat("world", "scaffold", "tower", "airplace", "nuker", "fastplace", "fastbreak", "fastuse",
                "autofarm", "autofish", "timer");
        cat("packets", "blink", "baritone", "autowalk", "groundspoof", "spoofer", "entityspoof",
                "chunkoverloader", "voidbearer", "guiinteract", "badpackets", "hackedclient",
                "inventory");
    }
    private static void cat(String category, String... families) {
        for (String f : families) CATEGORY.put(f, category);
    }

    private final Set<String> disabledFamilies = new HashSet<>();
    private final Set<String> disabledCategories = new HashSet<>();

    public CheckSettings(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "settings.yml");
        if (!f.isFile()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        if (!cfg.isConfigurationSection("checks")) return;

        Set<String> categories = new HashSet<>(CATEGORY.values());
        for (String key : cfg.getConfigurationSection("checks").getKeys(false)) {
            String name = key.toLowerCase(Locale.ROOT);
            String value = String.valueOf(cfg.get("checks." + key)).trim().toLowerCase(Locale.ROOT);
            boolean off = value.equals("disabled") || value.equals("false")
                    || value.equals("off") || value.equals("no");
            if (!off) continue;
            if (categories.contains(name)) {
                disabledCategories.add(name);
            } else {
                disabledFamilies.add(name);
            }
        }
    }

    public boolean familyEnabled(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        String bare = resolveFamily(key);
        if (bare != null && disabledFamilies.contains(bare)) return false;

        String category = bare != null ? CATEGORY.get(bare) : null;
        if (category != null && disabledCategories.contains(category)) return false;
        return true;
    }

    private String resolveFamily(String key) {
        if (CATEGORY.containsKey(key)) return key;
        if (key.length() > 1 && Character.isLetter(key.charAt(key.length() - 1))) {
            String stripped = key.substring(0, key.length() - 1);
            if (CATEGORY.containsKey(stripped)) return stripped;
        }
        for (String fam : CATEGORY.keySet()) {
            if (key.startsWith(fam)) return fam;
        }
        return key;
    }
}
