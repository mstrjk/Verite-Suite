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

@CheckInfo(name = "TimerA", description = "Short packet burst above real-time rate (burst timer).", decay = 0.02)
public final class TimerA extends Check implements PacketCheck, ConfidenceCheck {

    private static final long WINDOW_MS = 250L;
    private static final int MAX_PACKETS_PER_WINDOW = 8;
    private static final int SUSTAIN = 3;

    private final Deque<Long> times = new ArrayDeque<>();
    private int overWindows;
    private double innocence = 1.0;

    public TimerA(VeritePlayer player) {
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
        times.addLast(now);
        while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MS) {
            times.pollFirst();
        }

        if (times.size() > MAX_PACKETS_PER_WINDOW) {
            overWindows++;
            if (overWindows >= SUSTAIN) {
                int over = times.size() - MAX_PACKETS_PER_WINDOW;
                setInfo("packets=" + times.size() + " max=" + MAX_PACKETS_PER_WINDOW);
                innocence = Math.max(0.0, innocence - Math.min(1.0, over / 4.0) * 0.5);
            }
        } else {
            overWindows = Math.max(0, overWindows - 1);
            innocence = Math.min(1.0, innocence + 0.1);
        }
    }
}
