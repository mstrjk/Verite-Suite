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
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

final class SieveLang {

    private static final String[] PRIOR_LANGS = {"en", "es", "fr", "it", "pt", "de"};
    private static final Map<String, String> CODE = Map.of(
            "english", "en", "spanish", "es", "french", "fr",
            "italian", "it", "portuguese", "pt", "german", "de");

    private static final Map<String, float[]> PRIOR = new HashMap<>();

    private static double ctxWeight = 0.6;
    private static int iters = 3;
    private static double FIT_KNOWN = 50.0;
    private static double FIT_UNKNOWN = 0.1;

    private static final Map<String, Double> PRIOR_LANG = new HashMap<>(Map.of(
            "en", 0.529, "es", 0.194, "fr", 0.109, "pt", 0.091, "de", 0.046, "it", 0.031));

    private SieveLang() {}

    static void configure(double ctx, int it, double known, double unknown) {
        if (ctx >= 0 && ctx <= 1) ctxWeight = ctx;
        if (it >= 0 && it <= 20) iters = it;
        if (known > 0) FIT_KNOWN = known;
        if (unknown > 0) FIT_UNKNOWN = unknown;
    }

    static void load(Plugin plugin) {
        PRIOR.clear();
        try (InputStream raw = plugin.getResource("sieve/lexicon.bin")) {
            if (raw == null) {
                plugin.getLogger().info("[Verite] Si.EVE lang: lexicon.bin missing, prior disabled");
                return;
            }
            byte[] b = SieveSegment.decodeXzBytes(raw);
            decodePriors(b);
            plugin.getLogger().info("[Verite] Si.EVE lang: " + PRIOR.size() + " confusable priors.");
        } catch (Exception e) {
            plugin.getLogger().warning("[Verite] Si.EVE lang: prior load failed: " + e.getMessage());
            PRIOR.clear();
        }
    }

    private static void decodePriors(byte[] b) {
        int p = 6;
        int nl = p;
        while (b[nl] != '\n') nl++;
        String[] cols = new String(b, p, nl - p, StandardCharsets.UTF_8).split(",");
        p = nl + 1;
        int nWords = u32(b, p); p += 4;
        int fcLen = u32(b, p); p += 4;
        int freqStart = p + fcLen;
        int L = cols.length;
        int rowBytes = (L + 1) / 2;

        Map<String, String> nameToCode = Map.of("english","en","spanish","es","french","fr",
                "italian","it","portuguese","pt","german","de");
        int[] colToPrior = new int[L];
        for (int i = 0; i < L; i++) {
            String code = nameToCode.getOrDefault(cols[i], cols[i]);
            colToPrior[i] = -1;
            for (int j = 0; j < PRIOR_LANGS.length; j++) {
                if (PRIOR_LANGS[j].equals(code)) { colToPrior[i] = j; break; }
            }
        }

        StringBuilder prev = new StringBuilder();
        int fp = p;
        for (int wi = 0; wi < nWords; wi++) {
            int shared = b[fp++] & 0xFF;
            int sufLen = (b[fp] & 0xFF) | ((b[fp + 1] & 0xFF) << 8); fp += 2;
            String suffix = new String(b, fp, sufLen, StandardCharsets.UTF_8); fp += sufLen;
            prev.setLength(Math.min(shared, prev.length()));
            prev.append(suffix);
            String word = fold(prev.toString());

            int base = freqStart + wi * rowBytes;
            float[] rates = null;
            for (int i = 0; i < L; i++) {
                int slot = colToPrior[i];
                if (slot < 0) continue;
                int packed = b[base + (i >> 1)] & 0xFF;
                int fb = (i & 1) == 0 ? (packed >> 4) : (packed & 0x0F);
                if (fb == 0) continue;
                if (rates == null) rates = new float[PRIOR_LANGS.length];
                rates[slot] = dequantize(fb);
            }
            if (rates != null) PRIOR.put(word, rates);
        }
    }

    private static float dequantize(int nibble) {
        double v = (nibble - 1) / 14.0 * 7.7 - 3.0;
        return (float) Math.pow(10.0, v);
    }

    private static int u32(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }

    private static String fold(String s) {
        return Sieve.foldAccents(s.toLowerCase());
    }

    private static double[] likelihood(String tok, List<String> langs) {
        return softmax(logEvidence(tok, langs));
    }

    private static double[] logEvidence(String tok, List<String> langs) {
        double[] logp = new double[langs.size()];
        float[] prior = PRIOR.get(tok);
        double priorW = 1.0 - markedness(tok);
        for (int i = 0; i < langs.size(); i++) {
            String lang = langs.get(i);
            double rate;
            if (prior != null) {
                rate = priorRate(prior, lang);
            } else {
                rate = SieveSegment.knownIn(lang, tok) ? FIT_KNOWN : FIT_UNKNOWN;
            }
            logp[i] = Math.log(rate) + priorW * Math.log(priorLang(lang));
        }
        return logp;
    }

    private static double[] softmax(double[] logp) {
        double m = Double.NEGATIVE_INFINITY;
        for (double x : logp) m = Math.max(m, x);
        double[] p = new double[logp.length];
        double s = 0;
        for (int i = 0; i < logp.length; i++) {
            p[i] = Math.exp(logp[i] - m);
            s += p[i];
        }
        if (s > 0) for (int i = 0; i < p.length; i++) p[i] /= s;
        return p;
    }

    private static double priorLang(String lang) {
        Double p = PRIOR_LANG.get(CODE.get(lang));
        return p != null ? p : 0.01;
    }

    static void setPriorLang(Map<String, Double> overrides) {
        if (overrides != null) PRIOR_LANG.putAll(overrides);
    }

    private static double priorRate(float[] prior, String lang) {
        String code = CODE.get(lang);
        for (int i = 0; i < PRIOR_LANGS.length; i++) {
            if (PRIOR_LANGS[i].equals(code)) return Math.max(prior[i], FIT_UNKNOWN);
        }
        return FIT_UNKNOWN;
    }

    private static final double MARK_LO = 1.0;
    private static final double MARK_HI = 500.0;
    private static double markedness(String tok) {
        float[] prior = PRIOR.get(tok);
        if (prior == null) return 1.0;
        double mx = 0;
        for (float r : prior) if (r > mx) mx = r;
        if (mx <= MARK_LO) return 1.0;
        if (mx >= MARK_HI) return 0.0;

        return 1.0 - (Math.log(mx) - Math.log(MARK_LO)) / (Math.log(MARK_HI) - Math.log(MARK_LO));
    }

    private static void normalise(double[] p) {
        double s = 0;
        for (double x : p) s += x;
        if (s <= 0) return;
        for (int i = 0; i < p.length; i++) p[i] /= s;
    }

    private static double purityThreshold = 0.90;

    private static final double MISFIT_EJECT = 2.0;

    static List<double[]> label(String line) {
        List<String> langs = SieveSegment.languages();
        List<double[]> out = new ArrayList<>();
        String[] toks = line.split("\\s+");
        List<String> words = new ArrayList<>();
        for (String t : toks) if (!t.isEmpty()) words.add(t);
        if (langs.isEmpty() || words.isEmpty()) return out;

        int L = langs.size();
        double[][] ev = new double[words.size()][];
        double[] mark = new double[words.size()];
        blockWeight = new double[words.size()];
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);
            ev[i] = logEvidence(w, langs);

            if (w.length() < 2) {
                java.util.Arrays.fill(ev[i], 0.0);
                mark[i] = 0.0;
                blockWeight[i] = 0.0;
            } else {
                mark[i] = markedness(w);
                blockWeight[i] = BLOCK_WEIGHT_FLOOR + (1.0 - BLOCK_WEIGHT_FLOOR) * mark[i];
            }
        }

        double[][] result = new double[words.size()][];

        List<Integer> remaining = new ArrayList<>();
        int worked = Math.min(words.size(), MAX_LABEL_TOKENS);
        for (int i = 0; i < worked; i++) remaining.add(i);
        double[] uniform = new double[L];
        java.util.Arrays.fill(uniform, 1.0 / L);

        boolean trace = System.getProperty("sieve.trace") != null;
        int blockCount = 0;
        double[] dominant = null;
        while (!remaining.isEmpty()) {
            if (++blockCount > MAX_BLOCKS) {

                for (int tok : remaining) if (result[tok] == null) result[tok] = dominant != null ? dominant : uniform;
                break;
            }
            List<Integer> block = new ArrayList<>(remaining);
            List<Integer> ejected = new ArrayList<>();
            double[] dist = blockDist(block, ev, L);
            if (trace) {
                StringBuilder sb = new StringBuilder("BLOCK start: ");
                for (int t : block) sb.append(words.get(t)).append(" ");
                System.out.println(sb + "-> win=" + langs.get(argmax(dist)) + " conf=" + String.format("%.2f", conf(dist)));
            }

            for (int pass = 0; pass < MAX_PURIFY_PASSES && block.size() > 1; pass++) {
                int win = argmax(dist);
                List<Integer> keep = new ArrayList<>(block.size());
                boolean ejectedAny = false;
                for (int tok : block) {
                    if (mark[tok] < 0.5 && (ev[tok][argmax(ev[tok])] - ev[tok][win]) > MISFIT_EJECT) {
                        ejected.add(tok);
                        ejectedAny = true;
                        if (trace) System.out.println("   eject '" + words.get(tok) + "' from " + langs.get(win));
                    } else {
                        keep.add(tok);
                    }
                }
                if (!ejectedAny || keep.isEmpty()) break;
                block = keep;
                dist = blockDist(block, ev, L);
            }
            if (trace) {
                StringBuilder sb = new StringBuilder("   final block: ");
                for (int t : block) sb.append(words.get(t)).append(" ");
                System.out.println(sb + "=> " + langs.get(argmax(dist)) + " (" + String.format("%.0f%%", conf(dist)*100) + ")");
            }

            for (int tok : block) result[tok] = dist;

            if (dominant == null && conf(dist) > 1.0 / L + 1e-6) dominant = dist;

            if (ejected.size() == remaining.size()) {

                for (int tok : ejected) if (result[tok] == null) result[tok] = uniform;
                break;
            }
            remaining = ejected;
        }

        double[] tail = dominant != null ? dominant : uniform;
        for (int i = 0; i < words.size(); i++) {
            if (result[i] != null) out.add(result[i]);
            else out.add(i >= worked ? tail : uniform);
        }
        return out;
    }

    private static double[] blockDist(List<Integer> block, double[][] ev, int L) {
        double[] logp = new double[L];
        double wsum = 0;
        for (int tok : block) {
            double w = blockWeight[tok];
            for (int k = 0; k < L; k++) logp[k] += w * ev[tok][k];
            wsum += w;
        }

        if (wsum > 1e-9) for (int k = 0; k < L; k++) logp[k] /= wsum;
        return softmax(logp);
    }

    private static double[] blockWeight;
    private static final double BLOCK_WEIGHT_FLOOR = 0.02;

    private static final int MAX_PURIFY_PASSES = 4;
    private static final int MAX_BLOCKS = 6;
    private static final int MAX_LABEL_TOKENS = 64;

    private static double conf(double[] dist) {
        double m = 0; for (double x : dist) if (x > m) m = x; return m;
    }

    private static int argmax(double[] v) {
        int b = 0; for (int k = 1; k < v.length; k++) if (v[k] > v[b]) b = k; return b;
    }

    static String tokenLang(List<double[]> labelled, int i) {
        List<String> langs = SieveSegment.languages();
        if (i < 0 || i >= labelled.size() || langs.isEmpty()) return "";
        double[] p = labelled.get(i);
        int best = 0;
        for (int k = 1; k < p.length; k++) if (p[k] > p[best]) best = k;

        if (p[best] <= 1.0 / langs.size() + 1e-6) return "";
        return langs.get(best);
    }

    record Span(String lang, int start, int end) {}

    static List<Span> partitions(String line) {
        List<double[]> lab = label(line);
        List<Span> spans = new ArrayList<>();
        String cur = null;
        int start = 0;
        for (int i = 0; i < lab.size(); i++) {
            String lg = tokenLang(lab, i);
            if (cur == null) {
                cur = lg;
                start = i;
            } else if (!lg.equals(cur)) {
                spans.add(new Span(cur, start, i));
                cur = lg;
                start = i;
            }
        }
        if (cur != null) spans.add(new Span(cur, start, lab.size()));
        return spans;
    }

    record Confidence(String lang, double share, double margin) {}

    static Confidence confidence(String line) {
        List<String> langs = SieveSegment.languages();
        List<double[]> lab = label(line);
        if (langs.isEmpty() || lab.isEmpty()) return new Confidence("", 0, 0);
        double[] agg = new double[langs.size()];
        for (double[] p : lab) for (int k = 0; k < p.length; k++) agg[k] += p[k];
        for (int k = 0; k < agg.length; k++) agg[k] /= lab.size();
        int best = 0, second = -1;
        for (int k = 1; k < agg.length; k++) if (agg[k] > agg[best]) best = k;
        for (int k = 0; k < agg.length; k++) {
            if (k == best) continue;
            if (second < 0 || agg[k] > agg[second]) second = k;
        }
        double margin = second < 0 ? agg[best] : agg[best] - agg[second];
        return new Confidence(langs.get(best), agg[best], margin);
    }
}
