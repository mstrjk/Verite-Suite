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
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "SpeedD", description = "Velocity holds full speed through sharp turns.", decay = 0.02)
public final class SpeedD extends Check implements PacketCheck, ConfidenceCheck {

    private static final int IMPULSE_TICKS = 10;
    private static final double MIN_SPEED = 0.18;
    private static final double SHARP_TURN_COS = 0.5;
    private static final double RETENTION_FLOOR = 0.93;
    private static final int SUSTAIN = 3;

    private double innocence = 1.0;

    private double lastX, lastZ;
    private double lastDx, lastDz;
    private boolean hasLast;
    private boolean hasLastVec;
    private int butterTicks;
    private int teleportTimer;
    private int velocityTimer;

    public SpeedD(VeritePlayer player) {
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
        double speed = Math.sqrt(dx * dx + dz * dz);

        if (isExempt(s) || s.horizontalCollision || teleportTimer > 0 || velocityTimer > 0) {
            hasLastVec = false;
            butterTicks = 0;
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        if (!hasLastVec) {
            lastDx = dx;
            lastDz = dz;
            hasLastVec = true;
            return;
        }

        double lastSpeed = Math.sqrt(lastDx * lastDx + lastDz * lastDz);
        if (speed < MIN_SPEED || lastSpeed < MIN_SPEED) {
            lastDx = dx;
            lastDz = dz;
            innocence = Math.min(1.0, innocence + 0.2);
            return;
        }

        double dot = (dx * lastDx + dz * lastDz) / (speed * lastSpeed);
        double retention = speed / lastSpeed;

        boolean sharpTurn = dot < SHARP_TURN_COS;
        boolean keptSpeed = retention > RETENTION_FLOOR;

        if (sharpTurn && keptSpeed) {
            butterTicks++;
            if (butterTicks >= SUSTAIN) {
                setInfo("retention=" + formatOffset(retention) + " floor=" + formatOffset(RETENTION_FLOOR) + " dot=" + formatOffset(dot));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            butterTicks = Math.max(0, butterTicks - 1);
            innocence = Math.min(1.0, innocence + 0.25);
        }

        lastDx = dx;
        lastDz = dz;
    }

    private boolean isExempt(WorldSnapshot s) {
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return true;
        if (s.flying || s.gliding || s.riptiding || s.insideVehicle) return true;
        Material here = s.feetMaterial;
        Material below = s.belowMaterial;
        if (here == Material.WATER || below == Material.WATER || here == Material.LAVA) return true;
        if (here == Material.BUBBLE_COLUMN || below == Material.BUBBLE_COLUMN) return true;
        if (below == Material.ICE || below == Material.PACKED_ICE || below == Material.BLUE_ICE
                || below == Material.FROSTED_ICE) return true;
        if (below == Material.SLIME_BLOCK) return true;
        if (here == Material.COBWEB) return true;
        return false;
    }
}
