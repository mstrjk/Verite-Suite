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

import teacommontea.veritechasse.net.GameMode;
import teacommontea.veritechasse.net.Vec3i;
import teacommontea.veritechasse.check.BlockPlaceCheck;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "TowerB", description = "Towering with non-vanilla upward motion.", decay = 0.05)
public final class TowerB extends Check implements BlockPlaceCheck, ConfidenceCheck {

    private static final double VANILLA_JUMP_PEAK = 0.42;
    private static final double RISE_TOLERANCE = 0.08;
    private static final int SUSTAIN = 3;

    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private double lastPlaceY = Double.NaN;
    private int consecutiveColumn;
    private int suspectTicks;
    private double innocence = 1.0;

    public TowerB(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE) return;

        Vec3i pos = place.position;
        boolean sameColumn = pos.getX() == lastX && pos.getZ() == lastZ;

        double feetY = player.y;
        double risePerPlace = Double.isNaN(lastPlaceY) ? 0.0 : feetY - lastPlaceY;

        if (sameColumn && pos.getY() < feetY) {
            consecutiveColumn++;
        } else {
            consecutiveColumn = 0;
        }

        boolean towering = consecutiveColumn >= 2;
        double instantVel = player.y - player.lastY;

        boolean nonVanillaRise = towering
                && risePerPlace > 0.0
                && (instantVel > VANILLA_JUMP_PEAK + RISE_TOLERANCE
                    || (risePerPlace >= 0.9 && Math.abs(instantVel) < RISE_TOLERANCE));

        if (nonVanillaRise) {
            suspectTicks++;
            if (suspectTicks >= SUSTAIN) {
                setInfo("vel=" + formatOffset(instantVel) + " peak=" + formatOffset(VANILLA_JUMP_PEAK) + " rise=" + formatOffset(risePerPlace));
                innocence = Math.max(0.0, innocence - 0.4);
            }
        } else {
            suspectTicks = Math.max(0, suspectTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }

        lastX = pos.getX();
        lastZ = pos.getZ();
        lastPlaceY = feetY;
    }
}
