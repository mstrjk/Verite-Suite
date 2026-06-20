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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CheckInfo(name = "ScaffoldD", description = "Placed block has no support under or in front.", decay = 0.0)
public final class ScaffoldD extends Check implements ConfidenceCheck {

    private double innocence = 1.0;

    public ScaffoldD(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onPlace(Player bukkit, BlockPlace place) {
        if (!ScaffoldContext.isBridging(bukkit, place)) {
            innocence = 1.0;
            return;
        }

        int x = place.position.getX();
        int y = place.position.getY();
        int z = place.position.getZ();

        boolean under = bukkit.getWorld().getBlockAt(x, y - 1, z).getType().isSolid();
        boolean horizontalSupport =
                bukkit.getWorld().getBlockAt(x + 1, y, z).getType().isSolid()
                        || bukkit.getWorld().getBlockAt(x - 1, y, z).getType().isSolid()
                        || bukkit.getWorld().getBlockAt(x, y, z + 1).getType().isSolid()
                        || bukkit.getWorld().getBlockAt(x, y, z - 1).getType().isSolid();

        if (!under && !horizontalSupport) {
            setInfo("under=" + under + " horizontalSupport=" + horizontalSupport);
            innocence = Math.max(0.0, innocence - 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.4);
        }
    }
}
