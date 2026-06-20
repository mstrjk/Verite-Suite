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

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public final class SauverConfig {

    private static volatile YamlConfiguration yml = new YamlConfiguration();

    private SauverConfig() {}

    public static void load(File dataFolder) {
        File file = new File(dataFolder, "settings.yml");
        if (file.exists()) {
            yml = YamlConfiguration.loadConfiguration(file);
        } else {
            yml = new YamlConfiguration();
        }
    }

    public static boolean banAlts() {
        return yml.getBoolean("sauver.ban_alts", false);
    }

    public static List<String> muteCommandBlacklist() {
        List<String> def = List.of(
                "[+[($^):]]me[^^]", "[+[($^):]]say[^^]", "[+[($^):]]tell[^^]", "[+[($^):]]whisper[^^]",
                "[+[($^):]]reply[^^]", "[+[($^):]]pm[^^]", "[+[($^):]]message[^^]", "[+[($^):]]msg[^^]",
                "[+[($^):]]msgall[^^]", "[+[($^):]]tellall[^^]", "[+[($^):]]speak[^^]", "[+[($^):]]emsg[^^]",
                "[+[($^):]]epm[^^]", "[+[($^):]]etell[^^]", "[+[($^):]]ewhisper[^^]", "[+[($^):]]w[^^]",
                "[+[($^):]]m[^^]", "[+[($^):]]t[^^]", "[+[($^):]]r[^^]", "[+[($^):]]mail[^^]");
        List<String> got = yml.getStringList("sauver.mutes.command_blacklist");
        return got.isEmpty() ? def : got;
    }

    public static long warningExpire() {
        long d = SauverDuration.parse(yml.getString("sauver.warnings.expire_after", "7d"));
        return d <= 0 ? 7L * SauverDuration.DAY : d;
    }

    public static boolean useGroupWeights() {
        return yml.getBoolean("sauver.exempt.use_group_weights", false);
    }

    public static boolean permitSameWeight() {
        return yml.getBoolean("sauver.exempt.permit_same_weight", true);
    }

    public static boolean reduceToLimit() {
        return yml.getBoolean("sauver.durations.reduce_to_limit", true);
    }

    public static int dupeipScanLimit() {
        return yml.getInt("sauver.notify.dupeip_scan_limit", 20);
    }

    public static YamlConfiguration yaml() {
        return yml;
    }
}
