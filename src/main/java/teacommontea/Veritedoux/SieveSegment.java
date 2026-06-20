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

import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.tukaani.xz.XZInputStream;

final class SieveSegment {

    private record Lang(String label) {}

    private static final Lang[] REGISTRY = {
            new Lang("english"),

            new Lang("spanish"),

            new Lang("german"),
            new Lang("italian"),
            new Lang("french"),
            new Lang("portuguese"),
    };

    private static final List<SieveSegment> LANGS = new ArrayList<>();

    private final Map<String, Double> cost = new HashMap<>();
    private int maxWord = 1;
    private Map<String, List<String>> fpIndex;
    private String label = "";

    private SieveSegment() {}

    static boolean knownIn(String lang, String word) {
        for (SieveSegment seg : LANGS) {
            if (seg.label.equals(lang)) {
                return seg.cost.containsKey(word);
            }
        }
        return false;
    }

    static List<String> languages() {
        List<String> out = new ArrayList<>();
        for (SieveSegment seg : LANGS) {
            out.add(seg.label);
        }
        return out;
    }

    static void load(Plugin plugin) {
        LANGS.clear();
        try (InputStream raw = plugin.getResource("sieve/lexicon.bin")) {
            if (raw == null) {
                plugin.getLogger().warning("[Verite] Si.EVE segmentation: lexicon.bin missing, disabled");
                return;
            }
            byte[] blob = decodeXzBytes(raw);
            buildFromLexicon(plugin, blob);
        } catch (Exception e) {
            plugin.getLogger().warning("[Verite] Si.EVE segmentation: lexicon.bin failed to load: " + e.getMessage());
        }
        if (LANGS.isEmpty()) {
            plugin.getLogger().warning("[Verite] Si.EVE segmentation: no languages loaded, disabled");
        }
    }

    static byte[] decodeXzBytes(InputStream raw) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 24);
        try (XZInputStream xz = new XZInputStream(raw)) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = xz.read(buf)) > 0) out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static void buildFromLexicon(Plugin plugin, byte[] b) {
        int p = 0;
        p += 6;
        int nl = p;
        while (b[nl] != '\n') nl++;
        String[] cols = new String(b, p, nl - p, StandardCharsets.UTF_8).split(",");
        p = nl + 1;
        int nWords = readU32(b, p); p += 4;
        int fcLen = readU32(b, p); p += 4;
        int freqStart = p + fcLen;
        int L = cols.length;
        int rowBytes = (L + 1) / 2;

        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < cols.length; i++) colIndex.put(cols[i], i);
        Map<String, SieveSegment> byLang = new HashMap<>();
        for (Lang lang : REGISTRY) {
            if (colIndex.containsKey(lang.label())) {
                SieveSegment seg = new SieveSegment();
                seg.label = lang.label();
                byLang.put(lang.label(), seg);
            }
        }
        if (byLang.isEmpty()) return;

        StringBuilder prev = new StringBuilder();
        int fp = p;
        for (int wi = 0; wi < nWords; wi++) {
            int shared = b[fp++] & 0xFF;
            int sufLen = (b[fp] & 0xFF) | ((b[fp + 1] & 0xFF) << 8); fp += 2;
            String suffix = new String(b, fp, sufLen, StandardCharsets.UTF_8); fp += sufLen;
            prev.setLength(Math.min(shared, prev.length()));
            prev.append(suffix);
            String word = Sieve.foldAccents(prev.toString().toLowerCase());

            int base = freqStart + wi * rowBytes;
            for (Lang lang : REGISTRY) {
                SieveSegment seg = byLang.get(lang.label());
                if (seg == null) continue;
                int col = colIndex.get(lang.label());
                int packed = b[base + (col >> 1)] & 0xFF;
                int fb = (col & 1) == 0 ? (packed >> 4) : (packed & 0x0F);
                if (fb == 0) continue;
                double cost = costForFreq(fb, nWords);
                Double cur = seg.cost.get(word);
                if (cur == null || cost < cur) {
                    seg.cost.put(word, cost);
                    if (word.length() > seg.maxWord) seg.maxWord = word.length();
                }
            }
        }

        for (Lang lang : REGISTRY) {
            SieveSegment seg = byLang.get(lang.label());
            if (seg != null && !seg.cost.isEmpty()) {
                LANGS.add(seg);
                plugin.getLogger().info("[Verite] Si.EVE segmentation (" + seg.label + "): " + seg.cost.size() + " words.");
            }
        }
    }

    private static double costForFreq(int freqNibble, int nWords) {
        double logN = Math.log(Math.max(2, nWords));

        int effectiveRank = (int) Math.round(Math.pow(2.0, (15 - freqNibble)));
        return Math.log(effectiveRank * logN);
    }

    private static int readU32(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }

    static boolean ready() {
        return !LANGS.isEmpty();
    }

    static boolean fingerprintCollides(String fp, String slur) {
        for (SieveSegment seg : LANGS) {
            if (seg.collidesIn(fp, slur)) {
                return true;
            }
        }
        return false;
    }

    private boolean collidesIn(String fp, String slur) {
        if (fpIndex == null) {
            fpIndex = new HashMap<>();
            for (String w : cost.keySet()) {
                if (w.length() >= 3) {
                    fpIndex.computeIfAbsent(fpOf(w), k -> new ArrayList<>()).add(w);
                }
            }
        }
        List<String> bucket = fpIndex.get(fp);
        if (bucket == null) {
            return false;
        }
        for (String w : bucket) {
            if (!w.equals(slur)) {
                return true;
            }
        }
        return false;
    }

    private static String fpOf(String w) {
        if (w.length() < 3) {
            return w;
        }
        char[] mid = w.substring(1, w.length() - 1).toCharArray();
        java.util.Arrays.sort(mid);
        return w.charAt(0) + "|" + new String(mid) + "|" + w.charAt(w.length() - 1) + "|" + w.length();
    }

    private static final double OWNER_UNKNOWN = 20.0;

    static String owner(String line) {
        if (LANGS.isEmpty()) {
            return "";
        }
        String[] words = line.split("\\s+");
        String best = LANGS.get(0).label;
        double bestCost = Double.MAX_VALUE;
        for (SieveSegment seg : LANGS) {
            double total = 0;
            for (String w : words) {
                if (w.isEmpty()) continue;
                Double wc = seg.cost.get(w);
                total += (wc != null ? wc : OWNER_UNKNOWN);
            }
            if (total < bestCost - 1e-9) {
                bestCost = total;
                best = seg.label;
            }
        }
        return best;
    }

    static List<String> segmentLines(String lower) {
        List<String> out = new ArrayList<>();
        for (SieveSegment seg : LANGS) {
            String segged = seg.segmentLine(lower);
            if (!segged.equals(lower) && !out.contains(segged)) {
                out.add(segged);
            }
        }
        return out;
    }

    private String segmentLine(String lower) {
        StringBuilder sb = new StringBuilder();
        for (String w : lower.split("\\s+")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(segment(w));
        }
        return sb.toString();
    }

    private String segment(String token) {
        int n = token.length();
        if (n < 4 || n > 40) {
            return token;
        }
        double[] c = new double[n + 1];
        int[] back = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            double best = Double.MAX_VALUE;
            int bestK = i - 1;
            for (int k = Math.max(0, i - maxWord); k < i; k++) {
                Double wc = cost.get(token.substring(k, i));
                double cc = c[k] + (wc != null ? wc : (i - k) * 9.0 + 9.0);
                if (cc < best) {
                    best = cc;
                    bestK = k;
                }
            }
            c[i] = best;
            back[i] = bestK;
        }
        LinkedList<String> tiles = new LinkedList<>();
        int i = n;
        while (i > 0) {
            tiles.addFirst(token.substring(back[i], i));
            i = back[i];
        }
        return String.join(" ", tiles);
    }

    private static final Map<String, String> VOWELS_BY_LANG = new HashMap<>();

    static void setVowels(String lang, String vowels) {
        if (vowels == null) vowels = "";
        VOWELS_BY_LANG.put(lang, vowels);
    }

    static void clearVowels() {
        VOWELS_BY_LANG.clear();
    }

    private static String vowels() {
        java.util.LinkedHashSet<Character> set = new java.util.LinkedHashSet<>();
        for (String v : VOWELS_BY_LANG.values()) {
            for (int i = 0; i < v.length(); i++) set.add(v.charAt(i));
        }
        StringBuilder sb = new StringBuilder();
        for (char c : set) sb.append(c);
        return sb.toString();
    }

    static String vowelUnion() {
        return vowels();
    }

    static String deobfuscate(String token) {
        if (LANGS.isEmpty()) {
            return null;
        }

        Map<Character, Integer> freq = new HashMap<>();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                freq.merge(c, 1, Integer::sum);
            }
        }
        char filler = 0;
        int max = 0;
        for (Map.Entry<Character, Integer> e : freq.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                filler = e.getKey();
            }
        }
        if (max < 2) {
            return null;
        }

        StringBuilder dense = new StringBuilder();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c == filler) {
                int p = dense.length();
                if (p > 0 && (slots.isEmpty() || slots.get(slots.size() - 1) != p)) {
                    slots.add(p);
                }
            } else if (Character.isLetter(c)) {
                dense.append(c);
            }
        }
        String d = dense.toString();
        if (isKnown(d)) {
            return d;
        }

        if (d.length() > DEOB_MAX_DENSE || slots.size() > DEOB_MAX_SLOTS) {
            return null;
        }
        List<Integer> use = new ArrayList<>();
        for (int s : slots) {
            if (s > 0 && s < d.length()) {
                use.add(s);
            }
        }
        return repairSearch(d, use, 0, 0, vowels());
    }

    private static final int DEOB_MAX_DENSE = 14;
    private static final int DEOB_MAX_SLOTS = 8;

    private static String repairSearch(String s, List<Integer> slots, int idx, int inserted, String vowels) {
        if (isKnown(s)) {
            return s;
        }
        if (idx >= slots.size() || inserted >= 3) {
            return null;
        }
        int pos = slots.get(idx) + inserted;
        if (pos <= s.length()) {
            for (int v = 0; v < vowels.length(); v++) {
                String cand = s.substring(0, pos) + vowels.charAt(v) + s.substring(pos);
                String r = repairSearch(cand, slots, idx + 1, inserted + 1, vowels);
                if (r != null) {
                    return r;
                }
            }
        }
        return repairSearch(s, slots, idx + 1, inserted, vowels);
    }

    private static boolean isKnown(String w) {
        if (w.length() < 3) {
            return false;
        }
        for (SieveSegment seg : LANGS) {
            if (seg.cost.containsKey(w)) {
                return true;
            }
        }
        return false;
    }
}
