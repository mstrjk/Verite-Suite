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

import java.util.UUID;

final class SieveStone {

    static double start = 85;
    static double base = 150;
    static double cap = 200;
    static double minBand = 85;
    static long prelimMillis = 15 * 60 * 1000L;
    static double perSyllable = 0.3;
    static double penaltyStep = 0.15;
    static double penaltyMax = 1.0;

    static double[] anchorX = {0, 85, 150, 200};
    static double[] anchorY = {1.6, 1.4, 1.0, 0.9};

    private SieveStone() {}

    static void configure(SieveSettings s) {
        if (s == null) return;
        start = s.stoneStart;
        base = s.stoneBase;
        cap = s.stoneCap;
        minBand = s.stoneMinBand;
        prelimMillis = (long) (s.stonePrelimMinutes * 60_000L);
        perSyllable = s.stonePerSyllable;
        penaltyStep = s.stonePenaltyStep;
        penaltyMax = s.stonePenaltyMax;
        if (s.stoneAnchorX != null && s.stoneAnchorY != null
                && s.stoneAnchorX.length == s.stoneAnchorY.length && s.stoneAnchorX.length >= 2) {
            anchorX = s.stoneAnchorX;
            anchorY = s.stoneAnchorY;
        }
    }

    static double onClean(SieveStore.StoneRow row, int syllables, long nowMillis,
                          long[] outFirstSeen, boolean[] outSnapped, int[] outPenalty) {
        double score = row == null ? start : row.score();
        long firstSeen = row == null ? nowMillis : row.firstSeenMillis();
        boolean snapped = row != null && row.snapped();
        int penalty = row == null ? 0 : row.penalty();

        boolean prelimOver = nowMillis - firstSeen >= prelimMillis;
        if (prelimOver && !snapped) {

            score = Math.max(score, base);
            snapped = true;
        }
        if (snapped) {
            score = Math.min(cap, score + syllables * perSyllable);
        }

        if (score >= minBand) {
            penalty = 0;
        }
        outFirstSeen[0] = firstSeen;
        outSnapped[0] = snapped;
        outPenalty[0] = penalty;
        return score;
    }

    static double onFlag(SieveStore.StoneRow row, double drop, long nowMillis,
                         long[] outFirstSeen, boolean[] outSnapped, int[] outPenalty) {
        double score = row == null ? start : row.score();
        long firstSeen = row == null ? nowMillis : row.firstSeenMillis();
        boolean snapped = row != null && row.snapped();
        int penalty = row == null ? 0 : row.penalty();

        score = Math.max(0, score - drop);
        if (score < minBand) {
            penalty++;
        }
        outFirstSeen[0] = firstSeen;
        outSnapped[0] = snapped;
        outPenalty[0] = penalty;
        return score;
    }

    static double multiplier(double score, int penalty) {
        double m = curve(score);
        if (score < minBand && penalty > 0) {
            m += Math.min(penaltyMax, penalty * penaltyStep);
        }
        return m;
    }

    static double curve(double x) {
        double[] xs = anchorX, ys = anchorY;
        int n = xs.length;
        if (x <= xs[0]) return ys[0];
        if (x >= xs[n - 1]) return ys[n - 1];
        int i = 0;
        while (i < n - 1 && x > xs[i + 1]) i++;
        double h = xs[i + 1] - xs[i];
        if (h <= 0) return ys[i];
        double t = (x - xs[i]) / h;

        double m0 = tangent(xs, ys, i);
        double m1 = tangent(xs, ys, i + 1);
        double t2 = t * t, t3 = t2 * t;
        double h00 = 2 * t3 - 3 * t2 + 1;
        double h10 = t3 - 2 * t2 + t;
        double h01 = -2 * t3 + 3 * t2;
        double h11 = t3 - t2;
        return h00 * ys[i] + h10 * h * m0 + h01 * ys[i + 1] + h11 * h * m1;
    }

    private static double tangent(double[] xs, double[] ys, int i) {
        int n = xs.length;
        double dPrev = i > 0 ? (ys[i] - ys[i - 1]) / (xs[i] - xs[i - 1]) : 0;
        double dNext = i < n - 1 ? (ys[i + 1] - ys[i]) / (xs[i + 1] - xs[i]) : 0;
        if (i == 0) return dNext;
        if (i == n - 1) return dPrev;

        if (dPrev * dNext <= 0) return 0;
        double m = (dPrev + dNext) / 2;

        double lim = 3 * Math.min(Math.abs(dPrev), Math.abs(dNext));
        if (Math.abs(m) > lim) m = Math.signum(m) * lim;
        return m;
    }

    static int syllables(String message) {
        String vowels = vowelSet();
        if (vowels.isEmpty()) vowels = "aeiouy";
        int total = 0;
        for (String word : message.toLowerCase().split("\\s+")) {
            if (word.isEmpty()) continue;
            int groups = 0;
            boolean inVowel = false, sawLetter = false;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (Character.isLetter(c)) sawLetter = true;
                boolean v = vowels.indexOf(c) >= 0;
                if (v && !inVowel) groups++;
                inVowel = v;
            }
            if (sawLetter) total += Math.max(1, groups);
        }
        return total;
    }

    private static String vowelSet() {
        String v = SieveSegment.vowelUnion();
        return v == null ? "" : v;
    }
}
