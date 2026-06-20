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

package teacommontea.veritechasse.check.impl.autofarm;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "AutoFarmB", description = "Interacting with blocks outside the view direction (silent farm).", decay = 0.02)
public final class AutoFarmB extends Check implements ConfidenceCheck {

    private static final double EYE_HEIGHT = 1.62;
    private static final double MAX_ANGLE = 75.0;
    private static final int SUSTAIN = 3;

    private final InteractionSampler sampler;
    private int badInteractions;
    private double innocence = 1.0;

    public AutoFarmB(VeritePlayer player, InteractionSampler sampler) {
        super(player);
        this.sampler = sampler;
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void evaluate() {
        InteractionSampler.Interaction in = sampler.last();
        if (in == null) return;

        double ex = player.x;
        double ey = player.y + EYE_HEIGHT;
        double ez = player.z;

        double dx = (in.x + 0.5) - ex;
        double dy = (in.y + 0.5) - ey;
        double dz = (in.z + 0.5) - ez;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.8) return;

        double yawRad = Math.toRadians(in.yaw);
        double pitchRad = Math.toRadians(in.pitch);
        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

        double dot = (dx * lookX + dy * lookY + dz * lookZ) / dist;
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

        if (angle > MAX_ANGLE) {
            badInteractions++;
            if (badInteractions >= SUSTAIN) {
                setInfo("angle=" + formatOffset(angle) + " maxAngle=" + formatOffset(MAX_ANGLE) + " dist=" + formatOffset(dist));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            badInteractions = Math.max(0, badInteractions - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
