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

package teacommontea.veritechasse.check.impl.killaura;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.rotation.RotationData;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "KillAuraA", description = "Instant rotation snap during combat.", decay = 0.02)
public final class KillAuraA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double SNAP_YAW = 70.0;
    private static final double SETTLE_YAW = 4.0;
    private static final int COMBAT_WINDOW = 10;
    private static final int SUSTAIN = 2;

    private boolean armed;
    private int snaps;
    private double innocence = 1.0;

    public KillAuraA(VeritePlayer player) {
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
        if (!flying.hasRotationChanged()) return;

        boolean inCombat = player.currentTick() - player.lastAttackTick <= COMBAT_WINDOW;
        if (!inCombat) {
            armed = false;
            return;
        }

        RotationData r = player.rotations;

        if (armed && r.yawDelta < SETTLE_YAW) {
            armed = false;
            snaps++;
            if (snaps >= SUSTAIN) {
                setInfo("settleYaw=" + formatOffset(r.yawDelta) + " settleMax=" + formatOffset(SETTLE_YAW) + " snaps=" + String.valueOf(snaps));
                innocence = Math.max(0.0, innocence - 0.5);
            }
            return;
        }

        if (r.yawDelta > SNAP_YAW) {
            armed = true;
        } else if (r.yawDelta < SETTLE_YAW) {
            snaps = Math.max(0, snaps - 1);
            innocence = Math.min(1.0, innocence + 0.15);
        }
    }
}
