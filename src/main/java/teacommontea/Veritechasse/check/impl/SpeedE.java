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

@CheckInfo(name = "SpeedE", description = "Airborne horizontal speed decays slower than air friction.", decay = 0.02)
public final class SpeedE extends Check implements PacketCheck, ConfidenceCheck {

    private static final int IMPULSE_TICKS = 10;
    private static final double AIR_FRICTION = 0.91;
    private static final double AIR_ACCEL = 0.026;
    private static final double MIN_SPEED = 0.18;
    private static final double TOLERANCE = 0.012;
    private static final int SUSTAIN = 3;

    private double innocence = 1.0;

    private double lastX, lastZ;
    private boolean hasLast;
    private double lastHorizontal;
    private boolean hasLastHorizontal;
    private boolean wasOnGround = true;
    private int airTicks;
    private int overTicks;
    private int teleportTimer;
    private int velocityTimer;

    public SpeedE(VeritePlayer player) {
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
        boolean onGround = player.onGround;
        if (!hasLast) {
            lastX = x;
            lastZ = z;
            hasLast = true;
            wasOnGround = onGround;
            return;
        }

        double dx = x - lastX;
        double dz = z - lastZ;
        lastX = x;
        lastZ = z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (onGround || wasOnGround) {
            wasOnGround = onGround;
            airTicks = 0;
            overTicks = 0;
            lastHorizontal = horizontal;
            hasLastHorizontal = true;
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }
        wasOnGround = onGround;

        if (isExempt(s) || s.horizontalCollision || teleportTimer > 0 || velocityTimer > 0) {
            airTicks = 0;
            overTicks = 0;
            lastHorizontal = horizontal;
            hasLastHorizontal = true;
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        airTicks++;
        if (!hasLastHorizontal || airTicks < 2 || lastHorizontal < MIN_SPEED) {
            lastHorizontal = horizontal;
            hasLastHorizontal = true;
            return;
        }

        double maxNext = lastHorizontal * AIR_FRICTION + AIR_ACCEL;

        if (horizontal > maxNext + TOLERANCE) {
            overTicks++;
            if (overTicks >= SUSTAIN) {
                double overage = (horizontal - maxNext) / Math.max(maxNext, 0.05);
                setInfo("speed=" + formatOffset(horizontal) + " max=" + formatOffset(maxNext));
                innocence = Math.max(0.0, innocence - Math.min(1.0, overage) * 0.5);
            }
        } else {
            overTicks = Math.max(0, overTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }

        lastHorizontal = horizontal;
    }

    private boolean isExempt(WorldSnapshot s) {
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return true;
        if (s.flying || s.gliding || s.riptiding || s.insideVehicle) return true;
        Material here = s.feetMaterial;
        if (here == Material.WATER || here == Material.LAVA || here == Material.BUBBLE_COLUMN) return true;
        if (here == Material.COBWEB) return true;
        if (here == Material.LADDER || here == Material.VINE || here == Material.SCAFFOLDING
                || here == Material.WEEPING_VINES || here == Material.TWISTING_VINES) return true;
        return false;
    }
}
