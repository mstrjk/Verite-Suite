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

package teacommontea.veritechasse.check.impl.gui;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "GuiMoveA", description = "Moved through the world while a container GUI was open.", decay = 0.0)
public final class GuiMoveA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MOVE_THRESHOLD = 0.15;
    private static final long EXTERNAL_WINDOW_MS = 600L;
    private static final int SUSTAIN = 5;

    private double lastX, lastZ;
    private boolean hasLast;
    private int movingTicks;
    private double innocence = 1.0;

    public GuiMoveA(VeritePlayer player) {
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
        if (!flying.hasPositionChanged()) return;

        double x = flying.getLocation().getX();
        double z = flying.getLocation().getZ();
        if (!hasLast) {
            lastX = x;
            lastZ = z;
            hasLast = true;
            return;
        }
        double moved = Math.sqrt((x - lastX) * (x - lastX) + (z - lastZ) * (z - lastZ));
        lastX = x;
        lastZ = z;

        if (!player.guiOpen) {
            innocence = 1.0;
            movingTicks = 0;
            return;
        }

        if (moved <= MOVE_THRESHOLD) {
            movingTicks = 0;
            innocence = Math.min(1.0, innocence + 0.2);
            return;
        }

        long now = System.currentTimeMillis();
        boolean externallyCaused =
                now - player.lastVelocity < EXTERNAL_WINDOW_MS
                        || now - player.lastExternalEvent < EXTERNAL_WINDOW_MS
                        || player.guiMoveExempt;

        if (externallyCaused) {
            movingTicks = 0;
            innocence = Math.min(1.0, innocence + 0.2);
        } else {
            movingTicks++;
            if (movingTicks >= SUSTAIN) {
                setInfo("moved=" + formatOffset(moved) + " threshold=" + formatOffset(MOVE_THRESHOLD));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        }
    }
}
