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

@CheckInfo(name = "TimerD", description = "Packet intervals too consistent (constant multiplier).", decay = 0.02)
public final class TimerD extends Check implements PacketCheck, ConfidenceCheck {

    private static final int WINDOW = 30;
    private static final double MIN_CV = 0.08;
    private static final double FAST_MEAN_MS = 48.0;

    private final Deque<Long> intervals = new ArrayDeque<>(WINDOW);
    private long lastTime;
    private boolean hasLast;
    private double innocence = 1.0;

    public TimerD(VeritePlayer player) {
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
        double cv = Math.sqrt(var) / mean;

        if (cv < MIN_CV && mean < FAST_MEAN_MS) {
            setInfo("cv=" + String.format("%.3f", cv) + " minCv=" + String.format("%.3f", MIN_CV) + " mean=" + String.format("%.3f", mean));
            innocence = Math.max(0.0, innocence - 0.34);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
