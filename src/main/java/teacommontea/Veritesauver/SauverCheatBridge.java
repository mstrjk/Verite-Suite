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

import org.bukkit.Bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public final class SauverCheatBridge {

    private SauverCheatBridge() {}

    public static long flagCount(UUID uuid) {
        File file = detectionFile(uuid);
        if (file == null || !file.isFile()) {
            return 0L;
        }
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return countEntries(json);
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static File detectionFile(UUID uuid) {
        Sauver s = Sauver.instance();
        File base;
        if (s != null && s.plugin() != null) {
            base = s.plugin().getDataFolder();
        } else if (Bukkit.getPluginManager().getPlugin("Verite") != null) {
            base = Bukkit.getPluginManager().getPlugin("Verite").getDataFolder();
        } else {
            return null;
        }
        return new File(new File(base, "players"), uuid + ".json");
    }

    private static long countEntries(String json) {
        int arr = json.indexOf("\"entries\"");
        if (arr < 0) {
            return 0L;
        }
        int open = json.indexOf('[', arr);
        if (open < 0) {
            return 0L;
        }
        long count = 0;
        int from = open;
        while (true) {
            int next = json.indexOf("\"time\"", from + 1);
            if (next < 0) {
                break;
            }
            count++;
            from = next + 1;
        }
        return count;
    }
}
