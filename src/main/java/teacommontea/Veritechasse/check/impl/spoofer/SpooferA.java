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

package teacommontea.veritechasse.check.impl.spoofer;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "SpooferA", description = "Withholding transaction responses (ping spoof).", decay = 0.02)
public final class SpooferA extends Check implements PacketCheck, ConfidenceCheck {

    private static final int MAX_OUTSTANDING = 60;
    private static final int SUSTAIN = 5;

    private int overTicks;
    private double innocence = 1.0;

    public SpooferA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        int outstanding = player.getTransactions().outstanding();

        if (outstanding > MAX_OUTSTANDING) {
            overTicks++;
            if (overTicks >= SUSTAIN) {
                double over = (outstanding - MAX_OUTSTANDING) / 60.0;
                setInfo("outstanding=" + String.valueOf(outstanding) + " max=" + String.valueOf(MAX_OUTSTANDING) + " ticks=" + String.valueOf(overTicks));
                innocence = Math.max(0.0, innocence - Math.min(1.0, over) * 0.4);
            }
        } else {
            overTicks = Math.max(0, overTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
