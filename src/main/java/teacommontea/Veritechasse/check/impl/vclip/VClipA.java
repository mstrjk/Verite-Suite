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

package teacommontea.veritechasse.check.impl.vclip;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "VClipA", description = "Single-tick vertical clip through blocks.", decay = 0.02)
public final class VClipA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double CLIP_DELTA = 1.5;
    private static final double PRIOR_MOTION_LIMIT = 0.5;
    private static final int IMPULSE_TICKS = 12;

    private double lastY;
    private double lastDeltaY;
    private boolean hasLast;
    private boolean hasLastDelta;
    private int teleportTimer;
    private int velocityTimer;
    private double innocence = 1.0;

    public VClipA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markTeleport() {
        teleportTimer = IMPULSE_TICKS;
        hasLast = false;
        hasLastDelta = false;
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

        double y = flying.getLocation().getY();
        if (!hasLast) {
            lastY = y;
            hasLast = true;
            return;
        }
        double rawDelta = y - lastY;
        double deltaY = Math.abs(rawDelta);
        double priorMotion = hasLastDelta ? Math.abs(lastDeltaY) : 0.0;
        lastY = y;
        lastDeltaY = rawDelta;
        hasLastDelta = true;

        boolean suddenJump = deltaY > CLIP_DELTA && priorMotion < PRIOR_MOTION_LIMIT;

        if (!suddenJump) {
            innocence = Math.min(1.0, innocence + 0.2);
            return;
        }

        if (isExempt(s) || teleportTimer > 0 || velocityTimer > 0) {
            return;
        }

        setInfo("clip=" + formatOffset(deltaY) + " priorMotion=" + formatOffset(priorMotion));
        innocence = Math.max(0.0, innocence - 0.7);
    }

    private boolean isExempt(WorldSnapshot s) {
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return true;
        if (s.flying || s.gliding || s.riptiding || s.insideVehicle) return true;
        if (s.levitation || s.slowFalling) return true;

        Material here = s.feetMaterial;
        Material below = s.belowMaterial;
        if (isLiquid(here) || isLiquid(below)) return true;
        if (here == Material.BUBBLE_COLUMN || below == Material.BUBBLE_COLUMN) return true;
        if (here == Material.COBWEB || below == Material.COBWEB) return true;
        if (below == Material.SLIME_BLOCK || below == Material.HONEY_BLOCK) return true;
        if (isClimbable(here)) return true;
        if (s.feetTrapdoorOpen) return true;
        if (below == Material.POINTED_DRIPSTONE || here == Material.POINTED_DRIPSTONE) return true;
        return false;
    }

    private static boolean isLiquid(Material m) {
        return m == Material.WATER || m == Material.LAVA;
    }

    private static boolean isClimbable(Material m) {
        return m == Material.LADDER || m == Material.VINE || m == Material.SCAFFOLDING
                || m == Material.WEEPING_VINES || m == Material.TWISTING_VINES;
    }
}
