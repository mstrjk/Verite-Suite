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

package teacommontea.veritechasse.check.impl.length;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.Location;

@CheckInfo(name = "LengthA", description = "Interacted with a block or entity beyond vanilla reach.")
public final class LengthA extends Check implements PacketCheck {

    private static final double MAX_REACH = 6.0;
    private static final double EYE_HEIGHT = 1.62;

    public LengthA(VeritePlayer player) {
        super(player);
    }

    public void onContainerOpen(Location playerLoc, Location containerLoc) {
        if (containerLoc == null || playerLoc == null) return;
        if (containerLoc.getWorld() == null || playerLoc.getWorld() == null) return;
        if (!containerLoc.getWorld().equals(playerLoc.getWorld())) return;

        double dx = (containerLoc.getX() + 0.5) - playerLoc.getX();
        double dy = (containerLoc.getY() + 0.5) - (playerLoc.getY() + EYE_HEIGHT);
        double dz = (containerLoc.getZ() + 0.5) - playerLoc.getZ();
        report("container", Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    public void onBlockInteract(int bx, int by, int bz, String label) {
        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return;

        double dx = (bx + 0.5) - player.x;
        double dy = (by + 0.5) - (player.y + EYE_HEIGHT);
        double dz = (bz + 0.5) - player.z;
        report(label, Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    public void onEntityInteract(double tx, double ty, double tz) {
        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return;

        double dx = tx - player.x;
        double dy = ty - (player.y + EYE_HEIGHT);
        double dz = tz - player.z;
        report("entity", Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private void report(String label, double dist) {
        if (dist > MAX_REACH) {
            flag(label + " dist=" + String.format("%.1f", dist));
        }
    }
}
