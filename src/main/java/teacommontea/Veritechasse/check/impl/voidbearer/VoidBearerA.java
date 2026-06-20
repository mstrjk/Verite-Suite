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

package teacommontea.veritechasse.check.impl.voidbearer;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "VoidBearerA", description = "Descending endlessly below the world while alive.", decay = 0.0)
public final class VoidBearerA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double VOID_MARGIN = 64.0;
    private static final double DESCENT_THRESHOLD = -0.05;
    private static final int SUSTAIN_TICKS = 100;

    private double lastY;
    private boolean hasLast;
    private int descendingTicks;
    private double innocence = 1.0;

    public VoidBearerA(VeritePlayer player) {
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
        if (s.gameMode == GameMode.SPECTATOR || s.flying) {
            descendingTicks = 0;
            return;
        }

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

        boolean belowWorld = y < s.worldMinHeight - VOID_MARGIN;
        boolean descending = deltaY < DESCENT_THRESHOLD;

        if (belowWorld && descending) {
            descendingTicks++;
            if (descendingTicks > SUSTAIN_TICKS) {
                setInfo("y=" + String.format("%.3f", y) + " voidFloor=" + String.format("%.3f", s.worldMinHeight - VOID_MARGIN) + " descendingTicks=" + descendingTicks);
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            descendingTicks = Math.max(0, descendingTicks - 2);
            innocence = Math.min(1.0, innocence + 0.1);
        }
    }
}
