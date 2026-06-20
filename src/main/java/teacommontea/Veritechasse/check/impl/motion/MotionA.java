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

package teacommontea.veritechasse.check.impl.motion;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "MotionA", description = "Repeated constant vertical motion (replayed step).", decay = 0.02)
public final class MotionA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double EPSILON = 0.0005;
    private static final double MIN_MAGNITUDE = 0.02;
    private static final int REPEAT_REQUIRED = 3;
    private static final int IMPULSE_TICKS = 10;

    private double lastY;
    private double lastDeltaY;
    private boolean hasLast;
    private boolean hasLastDelta;
    private int repeatCount;
    private int teleportTimer;
    private double innocence = 1.0;

    public MotionA(VeritePlayer player) {
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

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle || s.levitation
                || teleportTimer > 0
                || isClimbable(s.feetMaterial) || s.feetMaterial == Material.WATER
                || s.belowMaterial == Material.WATER;

        if (exempt) {
            hasLastDelta = false;
            repeatCount = 0;
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        boolean repeated = hasLastDelta
                && Math.abs(deltaY) > MIN_MAGNITUDE
                && Math.abs(deltaY - lastDeltaY) < EPSILON;

        if (repeated) {
            repeatCount++;
            if (repeatCount >= REPEAT_REQUIRED) {
                setInfo("deltaY=" + formatOffset(deltaY) + " lastDeltaY=" + formatOffset(lastDeltaY) + " repeats=" + repeatCount);
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            repeatCount = 0;
            innocence = Math.min(1.0, innocence + 0.2);
        }

        lastDeltaY = deltaY;
        hasLastDelta = true;
    }

    private static boolean isClimbable(Material m) {
        return m == Material.LADDER || m == Material.VINE || m == Material.SCAFFOLDING
                || m == Material.WEEPING_VINES || m == Material.TWISTING_VINES
                || m == Material.CAVE_VINES || m == Material.CAVE_VINES_PLANT;
    }
}
