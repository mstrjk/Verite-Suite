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
import teacommontea.veritechasse.net.VeritePacketEvent.DiggingAction;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "FastBowA", description = "Released a bow before it could draw.", decay = 0.05)
public final class FastBowA extends Check implements PacketCheck, ConfidenceCheck {

    private static final int MIN_DRAW_TICKS = 3;

    private boolean drawing;
    private int drawStartTick;
    private int tick;
    private double innocence = 1.0;

    public FastBowA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.isFlying()) {
            tick++;
            return;
        }

        if (event.getPacketType() == VeritePacketType.USE_ITEM) {
            drawing = true;
            drawStartTick = tick;
            return;
        }

        if (event.getPacketType() == VeritePacketType.PLAYER_DIGGING) {
            if (event.getDiggingAction() != DiggingAction.RELEASE_USE_ITEM) return;
            if (!drawing) return;

            int drawTicks = tick - drawStartTick;
            drawing = false;
            if (drawTicks < MIN_DRAW_TICKS) {
                setInfo("drawTicks=" + String.valueOf(drawTicks) + " min=" + String.valueOf(MIN_DRAW_TICKS));
                innocence = Math.max(0.0, innocence - 0.6);
            } else {
                innocence = Math.min(1.0, innocence + 0.2);
            }
        }
    }
}
