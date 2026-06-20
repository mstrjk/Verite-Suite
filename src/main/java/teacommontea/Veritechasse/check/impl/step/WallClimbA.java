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
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "WallClimbA", description = "Climbing a non-climbable wall (spider).", decay = 0.02)
public final class WallClimbA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MIN_RISE = 0.08;
    private static final double SPIDER_DELTA = 0.2;
    private static final double EPSILON = 0.02;
    private static final int SUSTAIN = 3;
    private static final int IMPULSE_TICKS = 10;

    private double lastY;
    private boolean hasLast;
    private int riseTicks;
    private int teleportTimer;
    private double innocence = 1.0;

    public WallClimbA(VeritePlayer player) {
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
        boolean onGround = flying.isOnGround();
        if (!hasLast) {
            lastY = y;
            hasLast = true;
            return;
        }
        double deltaY = y - lastY;
        lastY = y;

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle
                || s.levitation || onGround || teleportTimer > 0
                || isClimbable(s.feetMaterial) || isLiquid(s.feetMaterial) || isLiquid(s.belowMaterial)
                || s.feetMaterial == Material.COBWEB || s.feetMaterial == Material.SCAFFOLDING;

        if (exempt) {
            riseTicks = Math.max(0, riseTicks - 1);
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        boolean spiderRise = deltaY > MIN_RISE && Math.abs(deltaY - SPIDER_DELTA) < EPSILON;
        if (spiderRise) {
            riseTicks++;
            if (riseTicks >= SUSTAIN) {
                setInfo("rise=" + formatOffset(deltaY) + " spider=" + formatOffset(SPIDER_DELTA));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            riseTicks = Math.max(0, riseTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }

    private static boolean isClimbable(Material m) {
        return m == Material.LADDER || m == Material.VINE || m == Material.SCAFFOLDING
                || m == Material.WEEPING_VINES || m == Material.TWISTING_VINES
                || m == Material.CAVE_VINES || m == Material.CAVE_VINES_PLANT;
    }

    private static boolean isLiquid(Material m) {
        return m == Material.WATER || m == Material.LAVA || m == Material.BUBBLE_COLUMN;
    }
}
