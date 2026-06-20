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

@CheckInfo(name = "TimerC", description = "Movement packets outrun real time (balance drift).", decay = 0.02)
public final class TimerC extends Check implements PacketCheck, ConfidenceCheck {

    private static final long MILLIS_PER_PACKET = 50L;
    private static final long MAX_BALANCE_MS = 1000L;
    private static final long DRIFT_FLAG_MS = 350L;

    private long lastTime;
    private boolean hasLast;
    private long balance;
    private double innocence = 1.0;

    public TimerC(VeritePlayer player) {
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

        long realElapsed = now - lastTime;
        lastTime = now;

        balance += realElapsed;
        balance -= MILLIS_PER_PACKET;

        if (balance > MAX_BALANCE_MS) balance = MAX_BALANCE_MS;
        if (balance < -MAX_BALANCE_MS) balance = -MAX_BALANCE_MS;

        if (balance < -DRIFT_FLAG_MS) {
            double severity = Math.min(1.0, (-balance - DRIFT_FLAG_MS) / 400.0);
            setInfo("balance=" + balance + " flagFloor=" + (-DRIFT_FLAG_MS));
            innocence = Math.max(0.0, innocence - 0.03 - severity * 0.08);
        } else {
            innocence = Math.min(1.0, innocence + 0.08);
        }
    }
}
