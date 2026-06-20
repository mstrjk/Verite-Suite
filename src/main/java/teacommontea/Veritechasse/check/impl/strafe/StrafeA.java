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

package teacommontea.veritechasse.check.impl.strafe;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "StrafeA", description = "Changing horizontal direction in the air beyond air-control.", decay = 0.02)
public final class StrafeA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double AIR_ACCEL = 0.026;
    private static final double MIN_SPEED = 0.15;
    private static final double TURN_TOLERANCE = 0.03;
    private static final int SUSTAIN = 3;
    private static final int IMPULSE_TICKS = 10;

    private double lastDx, lastDz;
    private boolean hasLastVec;
    private boolean wasAirborne;
    private int strafeTicks;
    private int teleportTimer;
    private int velocityTimer;
    private double innocence = 1.0;

    public StrafeA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markTeleport() {
        teleportTimer = IMPULSE_TICKS;
    }

    public void markVelocity() {
        velocityTimer = IMPULSE_TICKS;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;

        if (teleportTimer > 0) teleportTimer--;
        if (velocityTimer > 0) velocityTimer--;

        VeritePacketEvent flying = event;
        if (!flying.hasPositionChanged()) return;

        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        boolean airborne = !player.onGround;

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle || s.levitation
                || teleportTimer > 0 || velocityTimer > 0
                || isIce(s.belowMaterial) || s.feetMaterial == Material.COBWEB
                || s.belowMaterial == Material.SLIME_BLOCK;

        if (exempt || s.horizontalCollision || !airborne || !wasAirborne) {
            wasAirborne = airborne;
            hasLastVec = false;
            strafeTicks = Math.max(0, strafeTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
            return;
        }
        wasAirborne = airborne;

        if (!hasLastVec) {
            lastDx = dx;
            lastDz = dz;
            hasLastVec = true;
            return;
        }

        double perpChange = Math.abs(dx * (-lastDz) + dz * lastDx)
                / Math.max(1.0E-6, Math.sqrt(lastDx * lastDx + lastDz * lastDz));
        double speed = Math.sqrt(dx * dx + dz * dz);

        if (speed >= MIN_SPEED && perpChange > AIR_ACCEL + TURN_TOLERANCE) {
            strafeTicks++;
            if (strafeTicks >= SUSTAIN) {
                setInfo("perpChange=" + formatOffset(perpChange) + " max=" + formatOffset(AIR_ACCEL + TURN_TOLERANCE));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            strafeTicks = Math.max(0, strafeTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }

        lastDx = dx;
        lastDz = dz;
    }

    private static boolean isIce(Material m) {
        return m == Material.ICE || m == Material.PACKED_ICE || m == Material.BLUE_ICE || m == Material.FROSTED_ICE;
    }
}
