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

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.BlockPlaceCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;
import teacommontea.veritechasse.net.GameMode;

@CheckInfo(name = "FastPlaceB", description = "Placed a block out of reach.", decay = 0.02)
public final class FastPlaceB extends Check implements BlockPlaceCheck, teacommontea.veritechasse.check.ConfidenceCheck {

    private static final double BASE_INTERACTION_RANGE = 4.5;
    private static final double RANGE_TOLERANCE = 0.03;

    private double innocence = 1.0;

    @Override
    public double innocence() {
        return innocence;
    }

    public FastPlaceB(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {

        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;

        double bx = place.position.getX() + 0.5;
        double by = place.position.getY() + 0.5;
        double bz = place.position.getZ() + 0.5;

        double maxRange = BASE_INTERACTION_RANGE + RANGE_TOLERANCE;
        double[] eyeHeights = player.getPossibleEyeHeights();

        double best = Double.MAX_VALUE;
        for (double[] origin : new double[][]{
                {player.x, player.y, player.z},
                {player.lastX, player.lastY, player.lastZ}}) {
            for (double eye : eyeHeights) {
                double dx = bx - origin[0];
                double dy = by - (origin[1] + eye);
                double dz = bz - origin[2];
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < best) best = distance;
            }
        }

        if (best <= maxRange + 0.87) {
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        setInfo("reach=" + formatOffset(best) + " max=" + formatOffset(maxRange + 0.87));
        innocence = Math.max(0.0, innocence - 0.4);
    }
}
