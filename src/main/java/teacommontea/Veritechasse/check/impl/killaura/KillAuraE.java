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

@CheckInfo(name = "KillAuraE", description = "Pitch not a multiple of the player's learned mouse step.", decay = 0.02)
public final class KillAuraE extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MIN_DELTA = 0.05;
    private static final double MAX_DELTA = 20.0;
    private static final double MAX_DIVERGENCE = 0.22;
    private static final int COMBAT_WINDOW = 10;
    private static final int SUSTAIN = 4;

    private int badStreak;
    private double innocence = 1.0;

    public KillAuraE(VeritePlayer player) {
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

        RotationData r = player.rotations;
        if (!r.pitchGcd.isCalibrated()) return;
        if (player.currentTick() - player.lastAttackTick > COMBAT_WINDOW) return;

        double d = r.pitchDelta;
        if (d < MIN_DELTA || d > MAX_DELTA) return;

        double divergence = r.pitchGcd.divergence(d);
        if (divergence > MAX_DIVERGENCE) {
            badStreak++;
            if (badStreak >= SUSTAIN) {
                setInfo("divergence=" + formatOffset(divergence) + " maxDivergence=" + formatOffset(MAX_DIVERGENCE) + " pitchDelta=" + formatOffset(d));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            badStreak = Math.max(0, badStreak - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
