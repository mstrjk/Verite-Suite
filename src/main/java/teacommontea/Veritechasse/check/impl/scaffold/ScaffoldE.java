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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CheckInfo(name = "ScaffoldE", description = "Placed without clicking a valid block face.", decay = 0.0)
public final class ScaffoldE extends Check implements ConfidenceCheck {

    private double innocence = 1.0;

    public ScaffoldE(VeritePlayer player) {
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

        BlockFace face = place.getFace();
        int nx = place.position.getX() - face.getModX();
        int ny = place.position.getY() - face.getModY();
        int nz = place.position.getZ() - face.getModZ();

        Block against = bukkit.getWorld().getBlockAt(nx, ny, nz);
        if (against.getType().isAir()) {
            setInfo("againstFace=" + face + " neighbor=" + against.getType());
            innocence = Math.max(0.0, innocence - 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.4);
        }
    }
}
