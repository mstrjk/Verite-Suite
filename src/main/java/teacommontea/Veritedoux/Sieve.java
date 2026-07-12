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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Sieve {

    public enum Result { CLEAN, BLOCK, SELF_HARM, ABUSE, PROFANITY }

    private static teacommontea.eve.Eve EVE;
    private static int maxWords = 1;
    private static SieveStore store;
    private static SieveSettings settings = null;

    public static String blockMessage() {
        return settings == null ? "" : settings.blockMessage;
    }

    private static final int FP_MIN_LEN = 6;
    private static final java.util.Map<String, Result> FINGERPRINTS = new java.util.HashMap<>();

    private Sieve() {}

    private record Config(String file, String lang) {}
    private static final Config[] CONFIGS = {
            new Config("config_en.eve", "english"),
            new Config("config_es.eve", "spanish"),
            new Config("config_fr.eve", "french"),
            new Config("config_it.eve", "italian"),
            new Config("config_pt.eve", "portuguese"),
            new Config("config_de.eve", "german"),
            new Config("config_en_profanity.eve", "english"),
    };

    public static void load(Plugin plugin) {
        SieveSegment.clearVowels();
        settings = SieveSettings.load(plugin);
        SieveLang.configure(settings.langContextWeight, settings.langSmoothingPasses,
                settings.langKnownWeight, settings.langUnknownWeight);
        SieveStone.configure(settings);
        loadConfusables(plugin);
        maxWords = 1;

        for (Config config : CONFIGS) {
            boolean isProfanity = config.file().contains("profanity");
            if (isProfanity) {
                if (!settings.catProfanity) continue;
            } else if (!settings.languageEnabled(config.lang())) {
                continue;
            }
            String body = readConfigText(plugin, config.file());
            if (body == null) continue;
            SieveSegment.setVowels(config.lang(), eveVowels(body));
            maxWords = Math.max(maxWords, eveMaxWords(body));
        }
        EVE = loadEve(plugin);

        FINGERPRINTS.clear();
        int wordCount = 0;
        if (EVE != null) {
            for (teacommontea.eve.Eve.ConcreteWord cw : EVE.concreteWords()) {
                String w = cw.word();
                wordCount++;
                Result cat = cw.flag("sh", false) ? Result.SELF_HARM
                        : cw.flag("ea", false) ? Result.ABUSE
                        : cw.flag("pf", false) ? Result.PROFANITY : Result.BLOCK;
                if (w.length() >= FP_MIN_LEN && !SieveSegment.fingerprintCollides(fingerprint(w), w)) {
                    FINGERPRINTS.merge(fingerprint(w), cat, Sieve::worse);
                }
            }
        }
        int ruleCount = EVE == null ? 0 : EVE.ruleCount();
        plugin.getLogger().info("[Verite] Si.EVE loaded " + ruleCount + " rules (window " + maxWords
                + ", " + FINGERPRINTS.size() + " fingerprints from " + wordCount + " words)");
    }

    private static teacommontea.eve.Eve loadEve(Plugin plugin) {
        if (!teacommontea.eve.Eve.nativeAvailable()) {
            plugin.getLogger().warning("[Verite] EVE native lib unavailable ("
                    + teacommontea.eve.Eve.nativeError() + "), Si.EVE filter OFF");
            return null;
        }
        StringBuilder src = new StringBuilder();
        for (Config config : CONFIGS) {
            boolean isProfanity = config.file().contains("profanity");
            if (isProfanity) {
                if (!settings.catProfanity) continue;
            } else if (!settings.languageEnabled(config.lang())) {
                continue;
            }
            String body = readConfigText(plugin, config.file());
            if (body == null) continue;
            src.append(body).append('\n');
        }
        if (src.length() == 0) {
            plugin.getLogger().warning("[Verite] no config_*.eve found, Si.EVE filter OFF");
            return null;
        }
        try {
            teacommontea.eve.Eve eve = teacommontea.eve.Eve.parse(src.toString());
            plugin.getLogger().info("[Verite] EVE loaded " + eve.ruleCount() + " rules.");
            return eve;
        } catch (Exception e) {
            plugin.getLogger().warning("[Verite] EVE parse failed, Si.EVE filter OFF: " + e.getMessage());
            return null;
        }
    }

    private static String eveVowels(String body) {
        java.util.LinkedHashSet<Character> set = new java.util.LinkedHashSet<>();
        for (String line : body.split("\n")) {
            String t = line.strip();
            if (t.toLowerCase().indexOf("let function(") != 0) continue;
            int open = t.indexOf('('), close = t.indexOf(')', open);
            if (open < 0 || close < 0) continue;
            String name = t.substring(open + 1, close).trim();
            if (name.isEmpty() || !name.chars().allMatch(Character::isDigit)) continue;
            int be = t.toLowerCase().indexOf(" be ");
            if (be < 0) continue;
            for (String m : t.substring(be + 4).split("\\s+OR\\s+|\\s+or\\s+")) {
                m = m.strip();
                if (m.length() == 1 && Character.isLetter(m.charAt(0))) set.add(Character.toLowerCase(m.charAt(0)));
            }
        }
        StringBuilder sb = new StringBuilder();
        for (char c : set) sb.append(c);
        return sb.toString();
    }

    private static int eveMaxWords(String body) {
        int max = 1, gaps = 1;
        for (String line : body.split("\n")) {
            String t = line.strip();
            if (t.equalsIgnoreCase("GAP")) gaps++;
            else if (t.equalsIgnoreCase("HEAR rule") || t.equalsIgnoreCase("MATCH rule")) {
                max = Math.max(max, gaps); gaps = 1;
            } else {
                for (String op : t.split("\\s+OR\\s+|\\s+or\\s+|[()]")) {
                    int words = op.trim().isEmpty() ? 0 : op.trim().split("\\s+").length;
                    max = Math.max(max, words);
                }
            }
        }
        return Math.max(max, gaps);
    }

    private static String readConfigText(Plugin plugin, String name) {
        try {
            java.io.File edited = new java.io.File(plugin.getDataFolder(), name);
            if (edited.isFile()) {
                return new String(java.nio.file.Files.readAllBytes(edited.toPath()), StandardCharsets.UTF_8);
            }
            try (InputStream in = plugin.getResource("sieve/" + name)) {
                if (in == null) return null;
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String fingerprint(String w) {
        char[] mid = w.substring(1, w.length() - 1).toCharArray();
        java.util.Arrays.sort(mid);
        return w.charAt(0) + "|" + new String(mid) + "|" + w.charAt(w.length() - 1) + "|" + w.length();
    }

    private static Result worse(Result a, Result b) {
        if (a == Result.SELF_HARM || b == Result.SELF_HARM) return Result.SELF_HARM;
        if (a == Result.ABUSE || b == Result.ABUSE) return Result.ABUSE;
        if (a == Result.PROFANITY && b == Result.PROFANITY) return Result.PROFANITY;
        return Result.BLOCK;
    }

    public static void enableStore(SieveStore s) {
        store = s;
    }

    public static void shutdown() {
        if (store != null) {
            store.close();
            store = null;
        }
    }

    private static final int SEG_MIN_LEN = 5;

    private static final int MAX_SCAN_WORDS = 80;
    private static final int SCAN_STEP = 64;

    public static Result check(String message) {
        if (message == null || message.isEmpty() || EVE == null) {
            return Result.CLEAN;
        }

        Result worst = Result.CLEAN;
        for (String cand : candidates(message)) {
            Result r = checkAll(cand);
            if (r == Result.SELF_HARM || r == Result.ABUSE) {
                return r;
            }
            if (r != Result.CLEAN) {
                worst = (worst == Result.CLEAN) ? r : worse(worst, r);
            }
        }
        return worst;
    }

    private static Result checkAll(String lower) {
        String[] allWords = lower.split("\\s+");
        if (allWords.length > MAX_SCAN_WORDS) {
            Result worst = Result.CLEAN;
            for (int start = 0; start < allWords.length; start += SCAN_STEP) {
                int end = Math.min(start + MAX_SCAN_WORDS, allWords.length);
                StringBuilder w = new StringBuilder();
                for (int i = start; i < end; i++) { if (i > start) w.append(' '); w.append(allWords[i]); }
                Result r = checkWindow(w.toString());
                if (r != Result.CLEAN) worst = worst == Result.CLEAN ? r : worst;
                if (end == allWords.length) break;
            }
            return worst;
        }
        return checkWindow(lower);
    }

    private static Result checkWindow(String lower) {
        List<double[]> tokenLangs = SieveLang.label(lower);

        Result direct = matchLine(lower, 1, tokenLangs, lower);
        if (direct != Result.CLEAN) {
            return direct;
        }

        if (hasRepeat(lower)) {
            String capped = reduceRuns(lower, 2);
            if (!capped.equals(lower)) {
                Result r = matchLine(capped, 1, SieveLang.label(capped), lower);
                if (r != Result.CLEAN) return r;
            }
            String single = reduceRuns(lower, 1);
            if (!single.equals(lower) && !single.equals(capped)) {
                Result r = matchLine(single, 1, SieveLang.label(single), lower);
                if (r != Result.CLEAN) return r;
            }
        }

        if ((settings == null || settings.fingerprint) && !FINGERPRINTS.isEmpty()) {
            String[] fpWords = lower.split("\\s+");
            for (int wi = 0; wi < fpWords.length; wi++) {
                String w = fpWords[wi];
                if (w.length() >= FP_MIN_LEN && w.chars().allMatch(Character::isLetter)) {
                    Result fp = FINGERPRINTS.get(fingerprint(w));
                    if (fp != null && !langAllows(tokenLangAt(tokenLangs, wi), w)) {
                        return fp;
                    }
                }
            }
        }
        if (SieveSegment.ready()) {

            String raw = lower;
            List<String> candidates = new ArrayList<>();
            candidates.add(raw);
            for (String tok : raw.split("\\s+")) {
                candidates.add(tok);
            }
            if (settings == null || settings.deobfuscate) {
                for (String cand : candidates) {
                    String recovered = SieveSegment.deobfuscate(cand);
                    if (recovered != null) {
                        Result r = matchLine(recovered, 1, tokenLangs, lower);
                        if (r != Result.CLEAN) {
                            return r;
                        }
                    }
                }

                String dense = SieveSegment.denseStrip(raw);
                if (dense != null) {
                    Result r = matchLine(dense, 1, tokenLangs, lower);
                    if (r != Result.CLEAN) {
                        return r;
                    }
                }
            }

            if (settings == null || settings.segmentation) {
                for (String segged : SieveSegment.segmentLines(lower)) {
                    Result r = matchLine(segged, SEG_MIN_LEN, SieveLang.label(segged), lower);
                    if (r != Result.CLEAN) {
                        return r;
                    }
                }
            }
        }
        return Result.CLEAN;
    }

    private static String tokenLangAt(List<double[]> tokenLangs, int i) {
        return SieveLang.tokenLang(tokenLangs, i);
    }

    private static boolean inForeignSpan(List<double[]> tokenLangs, int i) {
        if (tokenLangs == null || i < 0 || i >= tokenLangs.size()) return false;
        String me = SieveLang.tokenLang(tokenLangs, i);
        if (me == null || me.isEmpty()) return false;
        if (i - 1 >= 0 && me.equals(SieveLang.tokenLang(tokenLangs, i - 1))) return true;
        if (i + 1 < tokenLangs.size() && me.equals(SieveLang.tokenLang(tokenLangs, i + 1))) return true;
        return false;
    }

    private static boolean langAllows(String lang, String word) {
        return lang != null && !lang.isEmpty()
                && SieveSegment.knownIn(lang, word) && !blockedByLang(lang, word);
    }

    private static boolean blockedByLang(String lang, String word) {
        if (EVE == null) return false;
        return !EVE.scan(word, word).isEmpty();
    }

    private static Result matchLine(String lower, int minSingleLen, List<double[]> tokenLangs, String vetoLine) {
        if (EVE == null) {
            return Result.CLEAN;
        }
        String[] words = lower.split("\\s+");
        Result worst = Result.CLEAN;
        for (int i = 0; i < words.length; i++) {
            StringBuilder chunk = new StringBuilder();
            for (int n = 0; n < maxWords && i + n < words.length; n++) {
                if (n > 0) {
                    chunk.append(' ');
                }
                chunk.append(words[i + n]);
                String c = chunk.toString();
                boolean singleWord = n == 0;
                if (singleWord && c.length() < minSingleLen) {
                    continue;
                }
                for (teacommontea.eve.Eve.Match m : EVE.scan(c, vetoLine)) {
                    Result r = categoryOf(m);
                    if (r == Result.SELF_HARM || r == Result.ABUSE) {
                        return r;
                    }
                    if (r == Result.BLOCK) {
                        worst = Result.BLOCK;
                    } else if (r == Result.PROFANITY && worst == Result.CLEAN) {
                        worst = Result.PROFANITY;
                    }
                }
            }
        }
        return worst;
    }

    private static Result categoryOf(teacommontea.eve.Eve.Match m) {
        if (m.flag("sh", false)) {
            return (settings == null || settings.catSelfHarm) ? Result.SELF_HARM : Result.CLEAN;
        }
        if (m.flag("ea", false)) {
            return (settings == null || settings.catAbuse) ? Result.ABUSE : Result.CLEAN;
        }
        if (m.flag("pf", false)) {
            return (settings != null && settings.catProfanity) ? Result.PROFANITY : Result.CLEAN;
        }
        return (settings == null || settings.catBlock) ? Result.BLOCK : Result.CLEAN;
    }

    private static final long DEDUP_WINDOW_MS = 750;
    private static final java.util.Map<String, long[]> DEDUP_STAMP = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Result> DEDUP_VERDICT = new java.util.concurrent.ConcurrentHashMap<>();

    private static String dedupKey(UUID player, String message) {
        return player + "\0" + message;
    }

    public static Result check(UUID player, String message) {
        String key = dedupKey(player, message);
        long now = System.currentTimeMillis();
        long[] prev = DEDUP_STAMP.get(key);
        if (prev != null && now - prev[0] < DEDUP_WINDOW_MS) {
            Result cached = DEDUP_VERDICT.get(key);
            if (cached != null) {
                return cached;
            }
        }

        Result r = check(message);
        if (r != Result.CLEAN && store != null && (settings == null || settings.logFlags)) {
            store.record(player, r, message);
        }
        r = applyStones(player, message, r);

        DEDUP_STAMP.put(key, new long[]{now});
        DEDUP_VERDICT.put(key, r);
        if (DEDUP_STAMP.size() > 512) {
            DEDUP_STAMP.entrySet().removeIf(e -> now - e.getValue()[0] > DEDUP_WINDOW_MS);
            DEDUP_VERDICT.keySet().retainAll(DEDUP_STAMP.keySet());
        }
        return r;
    }

    private static Result applyStones(UUID player, String message, Result r) {
        if (settings == null || !settings.stonesEnabled || store == null || player == null) {
            return r;
        }
        long now = System.currentTimeMillis();
        SieveStore.StoneRow row = store.loadStone(player);
        long[] fs = new long[1];
        boolean[] snap = new boolean[1];
        int[] pen = new int[1];
        double score;
        if (r == Result.CLEAN) {
            score = SieveStone.onClean(row, SieveStone.syllables(message), now, fs, snap, pen);
        } else {
            double drop = (r == Result.PROFANITY) ? 10 : 30;
            score = SieveStone.onFlag(row, drop, now, fs, snap, pen);
        }
        store.saveStone(player, score, fs[0], snap[0], pen[0]);

        if (r == Result.PROFANITY) {
            double m = SieveStone.multiplier(score, pen[0]);
            if (m >= 1.5) return Result.BLOCK;
            if (m <= 0.95) return Result.CLEAN;
        }
        return r;
    }

    public static boolean blocked(String message) {
        return check(message) != Result.CLEAN;
    }

    public static int count(UUID player) {
        return store == null ? 0 : store.count(player);
    }

    private static final java.util.Map<Integer, Character> CONFUSABLES = new java.util.HashMap<>();

    private static final java.util.Set<Integer> DELETE = new java.util.HashSet<>();

    private record Seq(String from, char[] to) {}
    private static final java.util.List<Seq> SEQS = new java.util.ArrayList<>();

    private static final int MAX_CANDIDATES = 32;

    private static boolean tablesLoaded() {
        return !CONFUSABLES.isEmpty() || !SEQS.isEmpty() || !DELETE.isEmpty();
    }

    private static String stripDeletes(String s) {
        boolean any = false;
        for (int i = 0; i < s.length() && !any; ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (DELETE.contains(cp)) any = true;
        }
        if (!any) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (!DELETE.contains(cp)) b.appendCodePoint(cp);
        }
        return b.toString();
    }

    private static java.util.List<String> candidates(String message) {
        boolean entity = settings == null || settings.entityStrip;
        boolean homo = settings == null || settings.homoglyphFold;
        String base = stripDeletes(foldAccents((entity ? stripEntities(message) : message).toLowerCase()));

        java.util.List<StringBuilder> cands = new java.util.ArrayList<>();
        cands.add(new StringBuilder());
        for (int i = 0; i < base.length(); ) {
            Seq hit = homo ? seqAt(base, i) : null;
            char[] targets;
            int adv;
            if (hit != null) {
                targets = hit.to();
                adv = hit.from().length();
            } else {
                int cp = base.codePointAt(i);
                adv = Character.charCount(cp);
                Character home = homo && cp <= 0xFFFF ? CONFUSABLES.get(cp) : null;
                if (home != null) {
                    targets = new char[]{home.charValue()};
                } else if (cp <= 0xFFFF) {
                    targets = new char[]{(char) cp};
                } else {

                    for (StringBuilder sb : cands) sb.appendCodePoint(cp);
                    i += adv;
                    continue;
                }
            }
            if (targets.length == 1 || cands.size() >= MAX_CANDIDATES) {
                char t = targets[0];
                for (StringBuilder sb : cands) sb.append(t);
            } else {
                java.util.List<StringBuilder> forked = new java.util.ArrayList<>(cands.size() * targets.length);
                for (char t : targets) {
                    if (forked.size() >= MAX_CANDIDATES) break;
                    for (StringBuilder sb : cands) {
                        if (forked.size() >= MAX_CANDIDATES) break;
                        forked.add(new StringBuilder(sb).append(t));
                    }
                }
                cands = forked;
            }
            i += adv;
        }
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        for (StringBuilder sb : cands) out.add(reduceRuns(sb.toString(), 2));
        return new java.util.ArrayList<>(out);
    }

    private static Seq seqAt(String base, int i) {
        for (Seq s : SEQS) {
            if (base.startsWith(s.from(), i)) {
                return s;
            }
        }
        return null;
    }

    private static void loadConfusables(Plugin plugin) {
        if (tablesLoaded()) {
            return;
        }
        try (InputStream in = plugin.getResource("sieve/confusables.txt")) {
            if (in == null) {
                return;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    if (line.startsWith("!DELETE ")) {
                        parseDelete(line.substring(8));
                    } else if (line.startsWith("!SEQ ")) {
                        parseSeq(line.substring(5));
                    } else if (line.charAt(0) == '=' && line.length() >= 2) {
                        parseClass(line);
                    }
                }
            }
            SEQS.sort((a, b) -> b.from().length() - a.from().length());
        } catch (Exception e) {
            plugin.getLogger().warning("[Verite] Si.EVE confusables table failed to load: " + e.getMessage());
        }
    }

    private static void parseDelete(String rest) {
        for (String tok : rest.trim().split("\\s+")) {
            if (tok.isEmpty()) continue;
            int dots = tok.indexOf("..");
            try {
                if (dots >= 0) {
                    int a = Integer.parseInt(tok.substring(0, dots), 16);
                    int z = Integer.parseInt(tok.substring(dots + 2), 16);
                    for (int cp = a; cp <= z; cp++) DELETE.add(cp);
                } else {
                    DELETE.add(Integer.parseInt(tok, 16));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static void parseSeq(String rest) {
        for (String tok : rest.trim().split("\\s+")) {
            int gt = tok.indexOf('>');
            if (gt <= 0 || gt + 1 >= tok.length()) continue;
            String from = tok.substring(0, gt);
            String targets = tok.substring(gt + 1).replace("|", "");
            if (targets.isEmpty()) continue;
            SEQS.add(new Seq(from, targets.toCharArray()));
        }
    }

    private static void parseClass(String line) {
        char proto = line.charAt(1);
        int sp = line.indexOf(' ', 2);
        if (sp < 0) return;
        for (String tok : line.substring(sp + 1).trim().split("\\s+")) {
            if (tok.isEmpty()) continue;
            try {
                CONFUSABLES.put(Integer.parseInt(tok, 16), proto);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static final java.util.regex.Pattern ENTITY =
            java.util.regex.Pattern.compile("&#x?[0-9a-fA-F]+;|&[a-zA-Z][a-zA-Z0-9]{1,31};");
    private static String stripEntities(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        return ENTITY.matcher(s).replaceAll(" ");
    }

    static String foldAccents(String s) {
        if (s.chars().allMatch(c -> c < 0x80)) {
            return s;
        }
        String d = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKD);
        StringBuilder b = new StringBuilder(d.length());
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static boolean hasRepeat(String s) {
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == s.charAt(i - 1) && Character.isLetter(c)) return true;
        }
        return false;
    }

    private static String reduceRuns(String s, int max) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            int run = 1;
            while (i + run < s.length() && s.charAt(i + run) == c) run++;
            int keep = Character.isLetter(c) ? Math.min(run, max) : run;
            for (int k = 0; k < keep; k++) b.append(c);
            i += run;
        }
        return b.toString();
    }
}
