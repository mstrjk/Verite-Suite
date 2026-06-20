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

package teacommontea.veritechasse.check.impl;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "TimerE", description = "Packet interval distribution faked (uniform jitter, fast mean).", decay = 0.02)
public final class TimerE extends Check implements PacketCheck, ConfidenceCheck {

    private static final int WINDOW = 40;
    private static final double FAST_MEAN_MS = 48.0;
    private static final double MIN_EXCESS_KURTOSIS = -1.0;

    private final Deque<Long> intervals = new ArrayDeque<>(WINDOW);
    private long lastTime;
    private boolean hasLast;
    private double innocence = 1.0;

    public TimerE(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        long now = System.currentTimeMillis();
        if (!hasLast) {
            lastTime = now;
            hasLast = true;
            return;
        }
        long delta = now - lastTime;
        lastTime = now;

        if (intervals.size() == WINDOW) intervals.pollFirst();
        intervals.addLast(delta);
        if (intervals.size() < WINDOW) return;

        double mean = 0;
        for (long v : intervals) mean += v;
        mean /= intervals.size();
        if (mean <= 0) return;

        double var = 0;
        for (long v : intervals) { double d = v - mean; var += d * d; }
        var /= intervals.size();
        if (var <= 1.0E-6) return;

        double m4 = 0;
        for (long v : intervals) { double d = v - mean; m4 += d * d * d * d; }
        m4 /= intervals.size();
        double excessKurtosis = (m4 / (var * var)) - 3.0;

        if (excessKurtosis < MIN_EXCESS_KURTOSIS && mean < FAST_MEAN_MS) {
            setInfo("kurtosis=" + String.format("%.3f", excessKurtosis) + " minKurtosis=" + String.format("%.3f", MIN_EXCESS_KURTOSIS) + " mean=" + String.format("%.3f", mean));
            innocence = Math.max(0.0, innocence - 0.34);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
