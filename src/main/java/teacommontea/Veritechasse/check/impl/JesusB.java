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
import org.bukkit.Material;

@CheckInfo(name = "JesusB", description = "Not sinking while in water.", decay = 0.02)
public final class JesusB extends Check implements PacketCheck, ConfidenceCheck {

    private static final double SINK_TOLERANCE = 0.005;

    private double lastY;
    private boolean hasLast;
    private double innocence = 1.0;

    public JesusB(VeritePlayer player) {
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

        double y = flying.getLocation().getY();
        if (!hasLast) {
            lastY = y;
            hasLast = true;
            return;
        }
        double deltaY = y - lastY;
        lastY = y;

        if (JesusExempt.exempt(s)) {
            innocence = 1.0;
            return;
        }

        boolean onWaterColumn = (s.feetMaterial == Material.WATER || s.belowMaterial == Material.WATER)
                && !s.inWater;

        if (onWaterColumn && deltaY > -SINK_TOLERANCE) {
            setInfo("feet=" + s.feetMaterial + " below=" + s.belowMaterial + " deltaY=" + formatOffset(deltaY) + " sinkTol=" + formatOffset(SINK_TOLERANCE));
            innocence = Math.max(0.0, innocence - 0.4);
        } else {
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
