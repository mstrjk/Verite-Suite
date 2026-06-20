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

package teacommontea.veritechasse.check.impl.scaffold;

import teacommontea.veritechasse.net.BlockFace;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.entity.Player;

@CheckInfo(name = "ScaffoldB", description = "Bridging while facing the direction of travel.", decay = 0.0)
public final class ScaffoldB extends Check implements ConfidenceCheck {

    private double innocence = 1.0;

    public ScaffoldB(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onPlace(Player bukkit, BlockPlace place) {
        if (ScaffoldContext.isBedrock(bukkit) || !ScaffoldContext.isBridging(bukkit, place)) {
            innocence = 1.0;
            return;
        }

        BlockFace face = place.getFace();
        double extendYaw = faceToYaw(opposite(face));
        if (Double.isNaN(extendYaw)) {
            innocence = 1.0;
            return;
        }

        double diff = Math.abs(wrap(player.yaw - extendYaw));

        if (diff < 90.0) {
            setInfo("facingDiff=" + formatOffset(diff) + " limit=" + formatOffset(90.0));
            innocence = Math.max(0.0, innocence - 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.4);
        }
    }

    private static BlockFace opposite(BlockFace f) {
        switch (f) {
            case NORTH: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.NORTH;
            case EAST: return BlockFace.WEST;
            case WEST: return BlockFace.EAST;
            default: return f;
        }
    }

    private static double faceToYaw(BlockFace f) {
        switch (f) {
            case SOUTH: return 0.0;
            case WEST: return 90.0;
            case NORTH: return 180.0;
            case EAST: return -90.0;
            default: return Double.NaN;
        }
    }

    private static double wrap(double deg) {
        deg %= 360.0;
        if (deg >= 180.0) deg -= 360.0;
        if (deg < -180.0) deg += 360.0;
        return deg;
    }
}
