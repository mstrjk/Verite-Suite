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

package teacommontea.veritechasse.check.impl;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "JesusC", description = "Moving across water faster than swim physics allows.", decay = 0.02)
public final class JesusC extends Check implements PacketCheck, ConfidenceCheck {

    private static final double BASE_SWIM_CEILING = 0.22;
    private static final double DEPTH_STRIDER_PER_LEVEL = 0.06;
    private static final double DOLPHIN_BONUS = 0.20;

    private double lastX, lastZ;
    private boolean hasLast;
    private double innocence = 1.0;

    public JesusC(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;

        VeritePacketEvent flying = event;
        if (!flying.hasPositionChanged()) return;

        double x = flying.getLocation().getX();
        double z = flying.getLocation().getZ();
        if (!hasLast) {
            lastX = x;
            lastZ = z;
            hasLast = true;
            return;
        }
        double dx = x - lastX;
        double dz = z - lastZ;
        lastX = x;
        lastZ = z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (!s.inWater || JesusExempt.exempt(s)) {
            innocence = Math.min(1.0, innocence + 0.3);
            return;
        }

        double ceiling = swimCeiling(s);
        if (horizontal > ceiling) {
            double overage = (horizontal - ceiling) / Math.max(ceiling, 0.05);
            setInfo("horizontal=" + formatOffset(horizontal) + " ceiling=" + formatOffset(ceiling));
            innocence = Math.max(0.0, innocence - Math.min(1.0, overage) * 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }

    private double swimCeiling(WorldSnapshot s) {
        double ceiling = BASE_SWIM_CEILING;
        ceiling += DEPTH_STRIDER_PER_LEVEL * s.depthStriderLevel;
        if (s.dolphinsGrace) {
            ceiling += DOLPHIN_BONUS;
        }
        return ceiling;
    }
}
