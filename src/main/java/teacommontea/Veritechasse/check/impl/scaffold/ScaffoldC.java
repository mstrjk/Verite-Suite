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

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CheckInfo(name = "ScaffoldC", description = "Meteor vine misplacement (placed under intent + fell).", decay = 0.0)
public final class ScaffoldC extends Check implements ConfidenceCheck {

    private double innocence = 1.0;
    private boolean armed;
    private double armedFeetY;

    public ScaffoldC(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onPlace(Player bukkit, BlockPlace place) {
        if (!ScaffoldContext.isBridging(bukkit, place)) {
            return;
        }

        int px = place.position.getX();
        int py = place.position.getY();
        int pz = place.position.getZ();

        Block intentCell = bukkit.getWorld().getBlockAt(px, py + 1, pz);
        boolean vineAtIntent = intentCell.getType() == Material.VINE
                || intentCell.getType() == Material.WEEPING_VINES
                || intentCell.getType() == Material.TWISTING_VINES
                || intentCell.getType() == Material.CAVE_VINES;

        if (vineAtIntent) {

            armed = true;
            armedFeetY = bukkit.getLocation().getY();
        }
    }

    public void onTick(Player bukkit) {
        if (!armed) return;

        if (bukkit.getLocation().getY() < armedFeetY - 0.4) {
            setInfo("fellY=" + formatOffset(armedFeetY - bukkit.getLocation().getY()) + " limit=" + formatOffset(0.4));
            innocence = 0.0;
            armed = false;
        } else if (bukkit.getLocation().getY() >= armedFeetY) {
            armed = false;
        }
    }
}
