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

package teacommontea.veritechasse.check.impl.autofarm;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "AutoFarmA", description = "Block interactions on a robotically steady cadence.", decay = 0.02)
public final class AutoFarmA extends Check implements ConfidenceCheck {

    private static final int MIN_SAMPLE = 12;
    private static final double MIN_CV = 0.06;
    private static final long MAX_MEAN_MS = 400L;
    private static final int SUSTAIN = 2;

    private final InteractionSampler sampler;
    private int botRuns;
    private double innocence = 1.0;

    public AutoFarmA(VeritePlayer player, InteractionSampler sampler) {
        super(player);
        this.sampler = sampler;
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void evaluate() {
        InteractionSampler.Interaction[] s = sampler.toArray();
        if (s.length < MIN_SAMPLE) return;

        int n = s.length - 1;
        double sum = 0;
        long[] deltas = new long[n];
        for (int i = 0; i < n; i++) {
            deltas[i] = s[i + 1].time - s[i].time;
            sum += deltas[i];
        }
        double mean = sum / n;
        if (mean <= 0 || mean > MAX_MEAN_MS) {
            botRuns = Math.max(0, botRuns - 1);
            innocence = Math.min(1.0, innocence + 0.2);
            return;
        }

        double var = 0;
        for (long d : deltas) {
            double dd = d - mean;
            var += dd * dd;
        }
        var /= n;
        double cv = Math.sqrt(var) / mean;

        if (cv < MIN_CV) {
            botRuns++;
            if (botRuns >= SUSTAIN) {
                setInfo("cv=" + formatOffset(cv) + " minCv=" + formatOffset(MIN_CV) + " meanMs=" + String.format("%.3f", mean));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            botRuns = Math.max(0, botRuns - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
