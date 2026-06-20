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
import teacommontea.veritechasse.engine.HorizontalPredictor;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "SpeedG", description = "Ice-like deceleration on a normal surface.", decay = 0.02)
public final class SpeedG extends Check implements PacketCheck, ConfidenceCheck {

    private static final int IMPULSE_TICKS = 10;
    private static final double MIN_SPEED = 0.16;
    private static final double SLIP_RATIO = 0.86;
    private static final int SUSTAIN = 4;

    private double innocence = 1.0;

    private double lastX, lastZ;
    private boolean hasLast;
    private double lastHorizontal;
    private boolean hasLastHorizontal;
    private int slipTicks;
    private int teleportTimer;
    private int velocityTimer;

    public SpeedG(VeritePlayer player) {
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
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (isExempt(s) || s.horizontalCollision || teleportTimer > 0 || velocityTimer > 0) {
            slipTicks = 0;
            lastHorizontal = horizontal;
            hasLastHorizontal = true;
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        if (!hasLastHorizontal || lastHorizontal < MIN_SPEED) {
            lastHorizontal = horizontal;
            hasLastHorizontal = true;
            return;
        }

        double ratio = horizontal / lastHorizontal;
        boolean decelerating = horizontal < lastHorizontal;

        if (decelerating && ratio > SLIP_RATIO) {
            slipTicks++;
            if (slipTicks >= SUSTAIN) {
                setInfo("ratio=" + formatOffset(ratio) + " slipFloor=" + formatOffset(SLIP_RATIO));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            slipTicks = Math.max(0, slipTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }

        lastHorizontal = horizontal;
    }

    private boolean isExempt(WorldSnapshot s) {
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return true;
        if (s.flying || s.gliding || s.riptiding || s.insideVehicle) return true;
        if (!s.bukkitOnGround) return true;
        if (HorizontalPredictor.blockFriction(s.belowMaterial) > 0.61f) return true;
        Material here = s.feetMaterial;
        Material below = s.belowMaterial;
        if (here == Material.WATER || below == Material.WATER || here == Material.LAVA) return true;
        if (here == Material.BUBBLE_COLUMN || below == Material.BUBBLE_COLUMN) return true;
        if (here == Material.COBWEB) return true;
        if (here == Material.SOUL_SAND || below == Material.SOUL_SAND) return true;
        return false;
    }
}
