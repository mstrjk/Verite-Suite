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

package teacommontea.veritechasse.check.impl.killaura;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.rotation.RotationData;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "KillAuraC", description = "Rotation speed too constant to be a human mouse.", decay = 0.02)
public final class KillAuraC extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MIN_DELTA = 0.5;
    private static final int SAMPLE = 8;
    private static final double MIN_CV = 0.01;
    private static final int SUSTAIN = 3;

    private final double[] samples = new double[SAMPLE];
    private int filled;
    private int badStreak;
    private double innocence = 1.0;

    public KillAuraC(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;
        VeritePacketEvent flying = event;
        if (!flying.hasRotationChanged()) return;

        RotationData r = player.rotations;
        if (r.yawDelta < MIN_DELTA) {
            badStreak = Math.max(0, badStreak - 1);
            return;
        }

        samples[filled % SAMPLE] = r.yawDelta;
        filled++;
        if (filled < SAMPLE) return;

        double mean = 0;
        for (double v : samples) mean += v;
        mean /= SAMPLE;
        if (mean <= 0) return;

        double var = 0;
        for (double v : samples) {
            double dd = v - mean;
            var += dd * dd;
        }
        var /= SAMPLE;
        double cv = Math.sqrt(var) / mean;

        if (cv < MIN_CV) {
            badStreak++;
            if (badStreak >= SUSTAIN) {
                setInfo("cv=" + formatOffset(cv) + " minCv=" + formatOffset(MIN_CV) + " mean=" + formatOffset(mean));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            badStreak = Math.max(0, badStreak - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
