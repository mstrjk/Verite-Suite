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

package teacommontea.veritechasse.engine.rotation;

public final class GcdCalibrator {

    private static final double STANDARD_QUANTUM = 0.15;
    private static final double MIN_USABLE = 0.05;
    private static final double MAX_USABLE = 30.0;
    private static final double MATCH_TOLERANCE = 0.02;
    private static final int STABLE_REQUIRED = 40;
    private static final double MIN_VALID_QUANTUM = 0.0096;

    private double quantum = STANDARD_QUANTUM;
    private double candidate;
    private boolean hasCandidate;
    private int stableHits;
    private boolean calibrated;

    public boolean isCalibrated() {
        return calibrated;
    }

    public double quantum() {
        return quantum;
    }

    public void feed(double delta) {
        if (delta < MIN_USABLE || delta > MAX_USABLE) return;

        if (!hasCandidate) {
            candidate = delta;
            hasCandidate = true;
            return;
        }

        double g = gcd(candidate, delta);
        if (g < MIN_VALID_QUANTUM) {
            candidate = delta;
            stableHits = 0;
            return;
        }

        if (Math.abs(g - candidate) <= MATCH_TOLERANCE) {
            stableHits++;
            candidate = (candidate + g) * 0.5;
            if (stableHits >= STABLE_REQUIRED) {
                quantum = candidate;
                calibrated = true;
            }
        } else {
            candidate = g;
            stableHits = 0;
            if (calibrated && g < quantum - MATCH_TOLERANCE) {
                calibrated = false;
            }
        }
    }

    public double divergence(double delta) {
        if (delta < MIN_USABLE || delta > MAX_USABLE) return 0.0;
        double q = quantum;
        if (q < MIN_VALID_QUANTUM) return 0.0;
        double ratio = delta / q;
        double off = Math.abs(ratio - Math.rint(ratio));
        return off;
    }

    private static double gcd(double a, double b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b > 1.0E-4) {
            double t = a - Math.floor(a / b) * b;
            a = b;
            b = t;
        }
        return a;
    }
}
