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

package teacommontea.veritechasse.check.impl.autoblock;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "AutoBlockA", description = "Attacking while actively blocking with a shield.", decay = 0.02)
public final class AutoBlockA extends Check implements PacketCheck, ConfidenceCheck {

    private static final int MIN_BLOCK_TICKS = 2;
    private static final int SUSTAIN = 2;

    private int hits;
    private double innocence = 1.0;

    public AutoBlockA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() != VeritePacketType.ATTACK) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;

        if (s.handRaised && s.handRaisedTime >= MIN_BLOCK_TICKS) {
            hits++;
            if (hits >= SUSTAIN) {
                setInfo("handRaisedTicks=" + String.valueOf(s.handRaisedTime) + " minBlock=" + String.valueOf(MIN_BLOCK_TICKS));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            hits = Math.max(0, hits - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
