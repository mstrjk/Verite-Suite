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

import teacommontea.veritechasse.engine.GroundDetector;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.util.Platform;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class ScaffoldContext {

    private ScaffoldContext() {
    }

    public static boolean isBedrock(Player p) {
        return Platform.isBedrock(p.getUniqueId());
    }

    public static boolean isBridging(Player p, BlockPlace place) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return false;
        if (p.isFlying() || p.isGliding() || p.isInsideVehicle()) return false;
        if (Platform.isBedrock(p.getUniqueId()) || place.getPlayer().bedrockClient) return false;

        int feetY = p.getLocation().getBlockY();
        int placedY = place.position.getY();

        if (placedY != feetY - 1) return false;
        return GroundDetector.isOnGround(p, p.getLocation().getY());
    }
}
