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
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "AutoClickerD", description = "Click interval distribution unnaturally flat (low kurtosis).", decay = 0.02)
public final class AutoClickerD extends Check implements PacketCheck, teacommontea.veritechasse.check.ConfidenceCheck {

    @Override
    public double innocence() {
        return innocence;
    }

    private static final int WINDOW = 20;
    private static final long IDLE_RESET_MS = 1500L;
    private static final long DIG_SUPPRESS_MS = 350L;
    private static final double FAST_MEAN_MS = 150.0;
    private static final double MIN_EXCESS_KURTOSIS = -1.35;
    private static final double MIN_STDDEV_MS = 3.0;

    private final Deque<Long> intervals = new ArrayDeque<>(WINDOW);
    private long lastClick = -1L;
    private long lastDig = -1L;
    private double innocence = 1.0;

    public AutoClickerD(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() == VeritePacketType.PLAYER_DIGGING) {
            lastDig = System.currentTimeMillis();
            intervals.clear();
            innocence = Math.min(1.0, innocence + 0.34);
            return;
        }
        if (event.getPacketType() != VeritePacketType.ANIMATION) return;

        long now = System.currentTimeMillis();
        if (lastDig >= 0 && now - lastDig < DIG_SUPPRESS_MS) {
            lastClick = now;
            return;
        }
        if (lastClick < 0) { lastClick = now; return; }

        long delta = now - lastClick;
        lastClick = now;

        if (delta > IDLE_RESET_MS) {
            intervals.clear();
            innocence = Math.min(1.0, innocence + 0.34);
            return;
        }

        if (intervals.size() == WINDOW) intervals.pollFirst();
        intervals.addLast(delta);
        if (intervals.size() < WINDOW) return;

        double mean = 0;
        for (long v : intervals) mean += v;
        mean /= intervals.size();
        if (mean <= 0 || mean > FAST_MEAN_MS) return;

        double var = 0;
        for (long v : intervals) { double d = v - mean; var += d * d; }
        var /= intervals.size();

        if (Math.sqrt(var) < MIN_STDDEV_MS) {
            innocence = Math.min(1.0, innocence + 0.34);
            return;
        }

        double m4 = 0;
        for (long v : intervals) { double d = v - mean; m4 += d * d * d * d; }
        m4 /= intervals.size();

        double excessKurtosis = (m4 / (var * var)) - 3.0;

        if (excessKurtosis < MIN_EXCESS_KURTOSIS) {
            setInfo("kurtosis=" + formatOffset(excessKurtosis) + " min=" + formatOffset(MIN_EXCESS_KURTOSIS));
            innocence = Math.max(0.0, innocence - 0.34);
        } else {
            innocence = Math.min(1.0, innocence + 0.34);
        }
    }
}
