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

package teacommontea.veritechasse.check.impl.nuker;

import teacommontea.veritechasse.check.BlockBreakCheck;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.BlockBreak;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "NukerB", description = "Broke a block outside the view direction.", decay = 0.05)
public final class NukerB extends Check implements BlockBreakCheck, ConfidenceCheck {

    private static final double EYE_HEIGHT = 1.62;
    private static final double BEHIND_ANGLE = 100.0;
    private static final double MAX_REACH = 6.0;
    private static final long GUI_LOCK_GRACE_MS = 200L;

    private double innocence = 1.0;

    public NukerB(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onBlockBreak(final BlockBreak blockBreak) {
        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return;

        if (player.guiOpen
                && System.currentTimeMillis() - player.lastGuiOpenChange > GUI_LOCK_GRACE_MS) {
            setInfo("guiOpen=true graceMs=" + String.valueOf(GUI_LOCK_GRACE_MS) + " sinceGuiChangeMs=" + String.valueOf(System.currentTimeMillis() - player.lastGuiOpenChange));
            innocence = 0.0;
            return;
        }

        double ex = player.x;
        double ey = player.y + EYE_HEIGHT;
        double ez = player.z;

        double bx = blockBreak.position.getX() + 0.5;
        double by = blockBreak.position.getY() + 0.5;
        double bz = blockBreak.position.getZ() + 0.5;

        double dx = bx - ex;
        double dy = by - ey;
        double dz = bz - ez;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.5) return;

        double yawRad = Math.toRadians(player.yaw);
        double pitchRad = Math.toRadians(player.pitch);
        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

        double dot = (dx * lookX + dy * lookY + dz * lookZ) / dist;
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

        if (angle > BEHIND_ANGLE || dist > MAX_REACH) {
            setInfo("angle=" + formatOffset(angle) + " maxAngle=" + formatOffset(BEHIND_ANGLE) + " dist=" + formatOffset(dist) + " maxReach=" + formatOffset(MAX_REACH));
            innocence = Math.max(0.0, innocence - 0.6);
        } else {
            innocence = Math.min(1.0, innocence + 0.15);
        }
    }
}
