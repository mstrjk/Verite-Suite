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

package teacommontea.veritedoux;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

final class SieveSettings {

    final Map<String, Boolean> languages = new HashMap<>();

    boolean catBlock = true, catSelfHarm = true, catAbuse = true, catProfanity = false;

    boolean homoglyphFold = true, entityStrip = true, deobfuscate = true,
            segmentation = true, fingerprint = true;
    boolean logFlags = true;
    String blockMessage = color("&#555555[&#B1C7F0Verité&#555555] &#FFFFFFYour message wasn't sent. Please keep chat friendly.");

    double langContextWeight = 0.6;
    int langSmoothingPasses = 3;
    double langKnownWeight = 50.0;
    double langUnknownWeight = 0.1;

    boolean stonesEnabled = false;
    double stoneStart = 85, stoneBase = 150, stoneCap = 200, stoneMinBand = 85;
    double stonePrelimMinutes = 15, stonePerSyllable = 0.3;
    double stonePenaltyStep = 0.15, stonePenaltyMax = 1.0;
    double[] stoneAnchorX = null, stoneAnchorY = null;

    private SieveSettings() {}

    static SieveSettings load(Plugin plugin) {
        SieveSettings st = new SieveSettings();
        File f = new File(plugin.getDataFolder(), "settings.yml");
        if (!f.isFile()) {

            st.languages.put("english", true);
            return st;
        }
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            if (y.isConfigurationSection("languages")) {
                for (String k : y.getConfigurationSection("languages").getKeys(false)) {
                    st.languages.put(k.toLowerCase(), y.getBoolean("languages." + k));
                }
            } else {
                st.languages.put("english", true);
            }
            st.catBlock = y.getBoolean("categories.block", true);
            st.catSelfHarm = y.getBoolean("categories.self_harm", true);
            st.catAbuse = y.getBoolean("categories.abuse", true);
            st.catProfanity = y.getBoolean("categories.profanity", false);
            st.homoglyphFold = y.getBoolean("evasion.homoglyph_fold", true);
            st.entityStrip = y.getBoolean("evasion.entity_strip", true);
            st.deobfuscate = y.getBoolean("evasion.deobfuscate", true);
            st.segmentation = y.getBoolean("evasion.segmentation", true);
            st.fingerprint = y.getBoolean("evasion.fingerprint", true);
            st.logFlags = y.getBoolean("log_flags", true);
            st.blockMessage = color(y.getString("block_message", st.blockMessage));
            st.langContextWeight = y.getDouble("language.context_weight", st.langContextWeight);
            st.langSmoothingPasses = y.getInt("language.smoothing_passes", st.langSmoothingPasses);
            st.langKnownWeight = y.getDouble("language.known_word_weight", st.langKnownWeight);
            st.langUnknownWeight = y.getDouble("language.unknown_word_weight", st.langUnknownWeight);
            st.stonesEnabled = y.getBoolean("stones.enabled", st.stonesEnabled);
            st.stoneStart = y.getDouble("stones.start", st.stoneStart);
            st.stoneBase = y.getDouble("stones.base", st.stoneBase);
            st.stoneCap = y.getDouble("stones.cap", st.stoneCap);
            st.stoneMinBand = y.getDouble("stones.min_band", st.stoneMinBand);
            st.stonePrelimMinutes = y.getDouble("stones.prelim_minutes", st.stonePrelimMinutes);
            st.stonePerSyllable = y.getDouble("stones.per_syllable", st.stonePerSyllable);
            st.stonePenaltyStep = y.getDouble("stones.penalty_step", st.stonePenaltyStep);
            st.stonePenaltyMax = y.getDouble("stones.penalty_max", st.stonePenaltyMax);
            st.stoneAnchorX = doubleList(y, "stones.curve_score");
            st.stoneAnchorY = doubleList(y, "stones.curve_multiplier");
        } catch (Exception e) {
            plugin.getLogger().warning("[Verite] Si.EVE settings.yml failed to load, using defaults: " + e.getMessage());
        }
        return st;
    }

    boolean languageEnabled(String code) {
        return languages.getOrDefault(code, false);
    }

    private static final java.util.regex.Pattern HEX = java.util.regex.Pattern.compile("&#([0-9a-fA-F]{6})");
    private static String color(String s) {
        if (s == null) return "";
        java.util.regex.Matcher m = HEX.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            StringBuilder rep = new StringBuilder("§x");
            for (char c : m.group(1).toCharArray()) rep.append('§').append(c);
            m.appendReplacement(out, rep.toString());
        }
        m.appendTail(out);
        return out.toString().replace('&', '§');
    }

    private static double[] doubleList(org.bukkit.configuration.file.YamlConfiguration y, String path) {
        java.util.List<Double> raw = y.getDoubleList(path);
        if (raw == null || raw.isEmpty()) return null;
        double[] out = new double[raw.size()];
        for (int i = 0; i < out.length; i++) out[i] = raw.get(i);
        return out;
    }
}
