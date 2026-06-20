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

package teacommontea.veritechasse.check.impl.autofish;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.VeritePlayer;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "AutoFishA", description = "Reeling in at a robotically consistent reaction time.", decay = 0.02)
public final class AutoFishA extends Check implements ConfidenceCheck {

    private static final int WINDOW = 6;
    private static final long MIN_HUMAN_MS = 120L;
    private static final double MIN_CV = 0.08;
    private static final int SUSTAIN = 2;

    private final Deque<Long> reactions = new ArrayDeque<>(WINDOW);
    private long biteTime = -1L;
    private int fastReels;
    private int robotRuns;
    private double innocence = 1.0;

    public AutoFishA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onBite() {
        biteTime = System.currentTimeMillis();
    }

    public void onReel() {
        if (biteTime < 0) return;
        long reaction = System.currentTimeMillis() - biteTime;
        biteTime = -1L;
        if (reaction < 0 || reaction > 2000) return;

        if (reaction < MIN_HUMAN_MS) {
            fastReels++;
            if (fastReels >= SUSTAIN) {
                setInfo("reactionMs=" + String.valueOf(reaction) + " minHumanMs=" + String.valueOf(MIN_HUMAN_MS) + " fastReels=" + String.valueOf(fastReels));
                innocence = Math.max(0.0, innocence - 0.5);
            }
            return;
        }
        fastReels = Math.max(0, fastReels - 1);

        if (reactions.size() == WINDOW) reactions.pollFirst();
        reactions.addLast(reaction);
        if (reactions.size() < WINDOW) return;

        double mean = 0;
        for (long v : reactions) mean += v;
        mean /= reactions.size();
        if (mean <= 0) return;

        double var = 0;
        for (long v : reactions) {
            double d = v - mean;
            var += d * d;
        }
        var /= reactions.size();
        double cv = Math.sqrt(var) / mean;

        if (cv < MIN_CV) {
            robotRuns++;
            if (robotRuns >= SUSTAIN) {
                setInfo("cv=" + formatOffset(cv) + " minCv=" + formatOffset(MIN_CV) + " meanMs=" + String.format("%.3f", mean));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            robotRuns = Math.max(0, robotRuns - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
