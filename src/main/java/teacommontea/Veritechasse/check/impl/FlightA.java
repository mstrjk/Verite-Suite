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
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.GroundDetector;
import teacommontea.veritechasse.engine.VerticalPredictor;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "FlightA", description = "Invalid vertical motion.", decay = 0.02)
public final class FlightA extends Check implements PacketCheck {

    private static final double EPSILON = 0.001;
    private static final double THRESHOLD = 0.03;
    private static final double MAX_ERROR = 0.02;
    private static final int SUSTAIN_LIMIT = 4;
    private static final int IMPULSE_TICKS = 10;
    private static final int WATER_EXIT_GRACE = 12;

    private static final double BEDROCK_GROUND_SLACK = 0.15;

    private double lastY;
    private boolean hasLastY;
    private double lastDeltaY;
    private boolean hasLastDelta;
    private int deviationTicks;

    private int teleportTimer;
    private int knockbackTimer;
    private int bounceTimer;
    private int waterExitTimer;

    public FlightA(VeritePlayer player) {
        super(player);
    }

    public void markTeleport() {
        teleportTimer = IMPULSE_TICKS;
    }

    public void markKnockback() {
        knockbackTimer = IMPULSE_TICKS;
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
        double y = flying.getLocation().getY();
        if (!hasLastY) {
            lastY = y;
            hasLastY = true;
            return;
        }

        double deltaY = y - lastY;
        lastY = y;

        if (s.inWater || s.swimming) {
            waterExitTimer = WATER_EXIT_GRACE;
        }

        if (isContinuouslyExempt(s) || waterExitTimer > 0) {
            softReset(deltaY);
            return;
        }

        if (s.belowMaterial == Material.SLIME_BLOCK || isBed(s.belowMaterial)) {
            bounceTimer = IMPULSE_TICKS;
        }

        if (teleportTimer > 0 || knockbackTimer > 0 || bounceTimer > 0) {
            softReset(deltaY);
            return;
        }

        if (onGroundBypass(s, x, z, y, deltaY)) {
            deviationTicks = Math.max(0, deviationTicks - 1);
            lastDeltaY = deltaY;
            hasLastDelta = true;
            return;
        }

        if (!hasLastDelta) {
            lastDeltaY = deltaY;
            hasLastDelta = true;
            return;
        }

        double predicted = VerticalPredictor.predictNextDeltaY(s, lastDeltaY);
        double error = Math.abs(deltaY - predicted);

        boolean movingError = error > EPSILON && Math.abs(deltaY) > THRESHOLD;
        boolean hoverError = error > MAX_ERROR;

        if (movingError || hoverError) {
            deviationTicks++;
            if (deviationTicks > SUSTAIN_LIMIT) {
                flag("dY=" + formatOffset(deltaY) + " pred=" + formatOffset(predicted)
                        + " err=" + formatOffset(error));
            }
        } else {
            deviationTicks = Math.max(0, deviationTicks - 1);
        }

        lastDeltaY = deltaY;
    }

    private boolean onGroundBypass(WorldSnapshot s, double x, double z, double y, double deltaY) {
        if (GroundDetector.isOnGround(s, x, z, y) || GroundDetector.isOnGround(s, x, z, y - deltaY)) {
            return true;
        }
        if (!player.bedrockClient && !teacommontea.veritechasse.util.Platform.isBedrock(player.getUuid())) {
            return false;
        }
        if (s.bukkitOnGround) {
            return true;
        }

        return GroundDetector.isOnGround(s, x, z, y - BEDROCK_GROUND_SLACK)
                || GroundDetector.isOnGround(s, x, z, y - deltaY - BEDROCK_GROUND_SLACK);
    }

    private void advanceUnseenTick() {
        if (!hasLastDelta) return;
        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        lastDeltaY = VerticalPredictor.predictNextDeltaY(s, lastDeltaY);
        if (Math.abs(lastDeltaY) < THRESHOLD) {
            lastDeltaY = 0.0;
        }
    }

    private void softReset(double deltaY) {
        deviationTicks = 0;
        lastDeltaY = deltaY;
        hasLastDelta = true;
    }

    private boolean isContinuouslyExempt(WorldSnapshot s) {
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return true;
        if (s.flying) return true;
        if (s.gliding || s.riptiding || s.insideVehicle) return true;
        if (s.slowFalling) return true;

        Material here = s.feetMaterial;
        Material below = s.belowMaterial;
        if (isLiquid(here) || isLiquid(below)) return true;
        if (isClimbable(here)) return true;
        if (s.feetTrapdoorOpen) return true;
        if (here == Material.BUBBLE_COLUMN || below == Material.BUBBLE_COLUMN) return true;
        if (here == Material.COBWEB || below == Material.COBWEB) return true;
        if (below == Material.HONEY_BLOCK) return true;
        if (here == Material.POWDER_SNOW || below == Material.POWDER_SNOW) return true;
        if (below == Material.SOUL_SAND) return true;
        if (below == Material.LILY_PAD || here == Material.LILY_PAD) return true;

        return false;
    }

    private void decayTimers() {
        if (teleportTimer > 0) teleportTimer--;
        if (knockbackTimer > 0) knockbackTimer--;
        if (bounceTimer > 0) bounceTimer--;
        if (waterExitTimer > 0) waterExitTimer--;
    }

    private static boolean isLiquid(Material m) {
        return m == Material.WATER || m == Material.LAVA;
    }

    private static boolean isClimbable(Material m) {
        return m == Material.LADDER || m == Material.VINE || m == Material.SCAFFOLDING
                || m == Material.WEEPING_VINES || m == Material.TWISTING_VINES;
    }

    private static boolean isBed(Material m) {
        return m.name().endsWith("_BED");
    }
}
