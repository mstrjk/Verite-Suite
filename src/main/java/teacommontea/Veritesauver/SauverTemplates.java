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

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SauverTemplates {

    public record Rung(String reason, String message, long duration, List<String> actions) {}

    public record Template(Entry.Type type, String name, String permission,
                           List<Rung> rungs, long expireLadder, boolean ipTemplate) {}

    private static final Map<String, Template> BAN = new LinkedHashMap<>();
    private static final Map<String, Template> MUTE = new LinkedHashMap<>();
    private static final Map<String, Template> WARN = new LinkedHashMap<>();
    private static final Map<String, Template> KICK = new LinkedHashMap<>();

    private SauverTemplates() {}

    public static void load(File dataFolder) {
        BAN.clear(); MUTE.clear(); WARN.clear(); KICK.clear();
        ConfigurationSection templates = SauverConfig.yaml().getConfigurationSection("sauver.templates");
        if (templates == null) {
            return;
        }
        loadType(templates, "ban-templates", Entry.Type.BAN, BAN);
        loadType(templates, "mute-templates", Entry.Type.MUTE, MUTE);
        loadType(templates, "warn-templates", Entry.Type.WARNING, WARN);
        loadType(templates, "kick-templates", Entry.Type.KICK, KICK);
    }

    private static void loadType(ConfigurationSection root, String path, Entry.Type type, Map<String, Template> into) {
        ConfigurationSection sec = root.getConfigurationSection(path);
        if (sec == null) {
            return;
        }
        for (String name : sec.getKeys(false)) {
            ConfigurationSection t = sec.getConfigurationSection(name);
            if (t == null) {
                continue;
            }
            String baseReason = t.getString("reason", "");
            String basePerm = t.getString("permission", null);
            long expireLadder = parseDur(t.getString("expire_ladder", "permanent"));
            boolean ipTemplate = t.getBoolean("ip_template", false);
            List<String> baseActions = t.getStringList("actions");

            List<Rung> rungs = new ArrayList<>();
            ConfigurationSection ladder = t.getConfigurationSection("ladder");
            if (ladder != null) {

                List<Rung> highToLow = new ArrayList<>();
                for (String rungKey : ladder.getKeys(false)) {
                    ConfigurationSection r = ladder.getConfigurationSection(rungKey);
                    if (r == null) {
                        continue;
                    }
                    highToLow.add(new Rung(
                        r.getString("reason", baseReason),
                        r.getString("message", null),
                        parseDur(r.getString("duration", "permanent")),
                        r.getStringList("actions").isEmpty() ? baseActions : r.getStringList("actions")));
                }
                for (int i = highToLow.size() - 1; i >= 0; i--) {
                    rungs.add(highToLow.get(i));
                }
            }
            if (rungs.isEmpty()) {
                rungs.add(new Rung(baseReason, null,
                        parseDur(t.getString("duration", "permanent")), baseActions));
            }
            into.put(name.toLowerCase(Locale.ROOT),
                    new Template(type, name, basePerm, rungs, expireLadder, ipTemplate));
        }
    }

    public static Template get(Entry.Type type, String name) {
        return mapFor(type).get(name.toLowerCase(Locale.ROOT));
    }

    private static Map<String, Template> mapFor(Entry.Type type) {
        return switch (type) {
            case BAN -> BAN;
            case MUTE -> MUTE;
            case WARNING -> WARN;
            case KICK -> KICK;
        };
    }

    public static Rung rungFor(Template template, int priorOffenses) {
        int idx = Math.min(priorOffenses, template.rungs().size() - 1);
        return template.rungs().get(Math.max(0, idx));
    }

    public static int templateId(Entry.Type type, String name) {
        int i = 1;
        for (String key : mapFor(type).keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    private static long parseDur(String s) {
        if (s == null || s.isBlank() || s.equalsIgnoreCase("permanent")) {
            return Entry.PERMANENT;
        }
        String[] parts = s.trim().split("\\s+");
        if (parts.length == 2) {
            long two = SauverDuration.parseTwoToken(parts[0], parts[1]);
            if (two != -1) {
                return two;
            }
        }
        long one = SauverDuration.parse(s);
        return one == -1 ? Entry.PERMANENT : one;
    }
}
