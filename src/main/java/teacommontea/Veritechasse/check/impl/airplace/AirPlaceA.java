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

package teacommontea.veritechasse.check.impl.airplace;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@CheckInfo(name = "AirPlaceA", description = "Placed into a breakable replaceable block.", decay = 0.0)
public final class AirPlaceA extends Check implements ConfidenceCheck {

    private double innocence = 1.0;

    public AirPlaceA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onPlace(Player bukkit, BlockPlace place) {
        if (!AirPlaceContext.active(bukkit)) {
            innocence = 1.0;
            return;
        }

        if (wasReplaceable(place.replacedMaterial)) {
            setInfo("replaced=" + place.replacedMaterial);
            innocence = Math.max(0.0, innocence - 0.34);
        } else {
            innocence = Math.min(1.0, innocence + 0.34);
        }
    }

    private static boolean wasReplaceable(Material m) {
        switch (m) {
            case SHORT_GRASS:
            case TALL_GRASS:
            case FERN:
            case LARGE_FERN:
            case DEAD_BUSH:
            case SEAGRASS:
            case TALL_SEAGRASS:
            case SNOW:
            case VINE:
            case GLOW_LICHEN:
            case HANGING_ROOTS:
            case WATER:
            case LAVA:
            case BUBBLE_COLUMN:
                return true;
            default:
                return false;
        }
    }
}
