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

package teacommontea.veritechasse.check.impl.step;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "AntiLevitationA", description = "Descending while under the levitation effect.", decay = 0.02)
public final class AntiLevitationA extends Check implements PacketCheck, ConfidenceCheck {

    private static final int THRESHOLD = 33;
    private static final int IMPULSE_TICKS = 10;

    private double lastY;
    private boolean hasLast;
    private int ignoredTicks;
    private int teleportTimer;
    private double innocence = 1.0;

    public AntiLevitationA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markTeleport() {
        teleportTimer = IMPULSE_TICKS;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;

        if (teleportTimer > 0) teleportTimer--;

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

        boolean exempt = !s.levitation || s.gliding || s.riptiding || s.insideVehicle
                || s.flying || teleportTimer > 0;
        if (exempt) {
            ignoredTicks = 0;
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        if (deltaY < 0.0) {
            ignoredTicks++;
        } else {
            ignoredTicks = 0;
        }

        if (ignoredTicks > THRESHOLD) {
            setInfo("deltaY=" + formatOffset(deltaY) + " descentTicks=" + ignoredTicks + " max=" + THRESHOLD);
            innocence = Math.max(0.0, innocence - 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.1);
        }
    }
}
