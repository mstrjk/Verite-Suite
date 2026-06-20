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

public final class ConfidenceScorer {

    public static final double INNOCENCE_THRESHOLD = 85.0;
    private static final double DECAY_RATIO = 0.6;
    private static final int SUSTAIN_TICKS = 5;

    private final ConfidenceCheck[] checks;
    private final double[] weights;
    private int sustainedBreaches;

    public ConfidenceScorer(ConfidenceCheck... checks) {
        this.checks = checks;
        this.weights = geometricWeights(checks.length);
    }

    private static final double SOLO_GUILTY_INNOCENCE = 0.15;

    private boolean lowConfidenceThisTick;

    public boolean isLowConfidence() {
        return lowConfidenceThisTick;
    }

    public boolean evaluate() {
        if (checks.length == 0) {
            lowConfidenceThisTick = false;
            return false;
        }

        boolean breached;
        if (checks.length == 1) {
            breached = clamp01(checks[0].innocence()) < SOLO_GUILTY_INNOCENCE;
        } else {
            breached = currentScore() < INNOCENCE_THRESHOLD;
        }
        lowConfidenceThisTick = breached;

        if (breached) {
            sustainedBreaches++;
            if (sustainedBreaches > SUSTAIN_TICKS) {
                sustainedBreaches = 0;
                return true;
            }
        } else {
            sustainedBreaches = Math.max(0, sustainedBreaches - 1);
        }
        return false;
    }

    private static final double MIN_BLEND = 0.5;

    public double currentScore() {
        double weighted = 0.0;
        double minInnocence = 1.0;
        for (int i = 0; i < checks.length; i++) {
            double inn = clamp01(checks[i].innocence());
            weighted += weights[i] * inn;
            if (inn < minInnocence) minInnocence = inn;
        }
        return (1.0 - MIN_BLEND) * weighted + MIN_BLEND * (minInnocence * 100.0);
    }

    public String lowestCheckName() {
        Check c = lowestCheck();
        return c == null ? "?" : c.getName();
    }

    public Check lowestCheck() {
        ConfidenceCheck worst = null;
        double lowest = Double.MAX_VALUE;
        for (ConfidenceCheck c : checks) {
            double inn = clamp01(c.innocence());
            if (inn < lowest) {
                lowest = inn;
                worst = c;
            }
        }
        return worst instanceof Check ck ? ck : null;
    }

    static double[] geometricWeights(int n) {
        double[] w = new double[n];
        if (n == 0) return w;
        if (n == 1) {
            w[0] = 100.0;
            return w;
        }

        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double exponent = (n - 1) - i;
            w[i] = Math.pow(DECAY_RATIO, exponent);
            sum += w[i];
        }
        for (int i = 0; i < n; i++) {
            w[i] = w[i] / sum * 100.0;
        }
        return w;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        return Math.min(v, 1.0);
    }
}
