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

package teacommontea.veritechasse.check.impl.killaura;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.entity.TrackedEntity;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "KillAuraB", description = "Attacked an entity far outside the view direction.", decay = 0.02)
public final class KillAuraB extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MAX_ANGLE = 75.0;
    private static final double EYE_HEIGHT = 1.62;
    private static final int SUSTAIN = 2;

    private int overHits;
    private double innocence = 1.0;

    public KillAuraB(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() != VeritePacketType.ATTACK) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return;

        TrackedEntity target = player.entityTracker.get(event.getInteractEntityId());
        if (target == null) return;

        double ex = player.x;
        double ey = player.y + EYE_HEIGHT;
        double ez = player.z;
        double dx = target.x - ex;
        double dy = (target.y + target.height / 2.0) - ey;
        double dz = target.z - ez;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.5) return;

        double yawRad = Math.toRadians(player.yaw);
        double pitchRad = Math.toRadians(player.pitch);
        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

        double dot = (dx * lookX + dy * lookY + dz * lookZ) / dist;
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

        if (angle > MAX_ANGLE) {
            overHits++;
            if (overHits >= SUSTAIN) {
                setInfo("angle=" + formatOffset(angle) + " maxAngle=" + formatOffset(MAX_ANGLE) + " dist=" + formatOffset(dist));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            overHits = Math.max(0, overHits - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
