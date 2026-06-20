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
import teacommontea.veritechasse.net.VeritePacketEvent.DiggingAction;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.GroundDetector;
import teacommontea.veritechasse.engine.HorizontalPredictor;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "SpeedA", description = "Moving too fast horizontally.", decay = 0.02)
public final class SpeedA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double TOLERANCE = 0.001;
    private static final int IMPULSE_TICKS = 10;

    private double innocence = 1.0;

    @Override
    public double innocence() {
        return innocence;
    }

    private double lastX, lastZ;
    private boolean hasLast;
    private double lastHorizontal;
    private boolean hasLastHorizontal;

    private int teleportTimer;
    private int velocityTimer;

    public SpeedA(VeritePlayer player) {
        super(player);
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

        decayTimers();

        VeritePacketEvent flying = event;
        if (!flying.hasPositionChanged()) {
            advanceUnseenTick();
            return;
        }

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

        if (isExempt(s)) {
            softReset(horizontal);
            return;
        }

        if (teleportTimer > 0 || velocityTimer > 0) {
            softReset(horizontal);
            return;
        }

        if (!hasLastHorizontal) {
            lastHorizontal = horizontal;
            hasLastHorizontal = true;
            return;
        }

        boolean onGround = GroundDetector.isOnGround(s, x, z, flying.getLocation().getY());
        double maxNext = HorizontalPredictor.maxNextHorizontal(s, lastHorizontal, onGround);

        if (horizontal > maxNext + TOLERANCE) {
            double overageRatio = (horizontal - maxNext) / Math.max(maxNext, 0.05);
            double drop = Math.min(1.0, overageRatio);
            setInfo("speed=" + formatOffset(horizontal) + " max=" + formatOffset(maxNext));
            innocence = Math.max(0.0, innocence - drop * 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.25);
        }

        lastHorizontal = horizontal;
    }

    private void advanceUnseenTick() {
        if (!hasLastHorizontal) return;
        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        lastHorizontal = HorizontalPredictor.maxNextHorizontal(s, lastHorizontal, s.bukkitOnGround) * 0.91;
    }

    private void softReset(double horizontal) {
        innocence = 1.0;
        lastHorizontal = horizontal;
        hasLastHorizontal = true;
    }

    private boolean isExempt(WorldSnapshot s) {
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return true;
        if (s.flying) return true;
        if (s.gliding || s.riptiding || s.insideVehicle) return true;
        if (s.dolphinsGrace || s.levitation) return true;

        Material here = s.feetMaterial;
        Material below = s.belowMaterial;
        if (isLiquid(here) || isLiquid(below)) return true;
        if (here == Material.BUBBLE_COLUMN || below == Material.BUBBLE_COLUMN) return true;
        if (here == Material.COBWEB) return true;
        if (here == Material.POWDER_SNOW) return true;
        if (isClimbable(here)) return true;

        return false;
    }

    private void decayTimers() {
        if (teleportTimer > 0) teleportTimer--;
        if (velocityTimer > 0) velocityTimer--;
    }

    private static boolean isLiquid(Material m) {
        return m == Material.WATER || m == Material.LAVA;
    }

    private static boolean isClimbable(Material m) {
        return m == Material.LADDER || m == Material.VINE || m == Material.SCAFFOLDING
                || m == Material.WEEPING_VINES || m == Material.TWISTING_VINES;
    }
}
