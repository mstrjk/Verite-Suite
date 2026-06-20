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

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "SpeedH", description = "Sustained horizontal velocity above the empirical ceiling.", decay = 0.02)
public final class SpeedH extends Check implements PacketCheck, ConfidenceCheck {

    private static final int WINDOW = 20;
    private static final double BASE_CEILING = 0.45;
    private static final double PER_SPEED_LEVEL = 0.06;
    private static final int IMPULSE_TICKS = 10;

    private final Deque<Double> window = new ArrayDeque<>(WINDOW);
    private double lastX, lastZ;
    private boolean hasLast;
    private double innocence = 1.0;

    @Override
    public double innocence() {
        return innocence;
    }

    private int teleportTimer;
    private int velocityTimer;

    public SpeedH(VeritePlayer player) {
        super(player);
    }

    public void markTeleport() {
        teleportTimer = IMPULSE_TICKS;
    }

    public void markVelocity() {
        velocityTimer = IMPULSE_TICKS;
    }

    public void exempt(int ticks) {
        if (ticks > velocityTimer) velocityTimer = ticks;
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

        if (isExempt(s) || teleportTimer > 0 || velocityTimer > 0) {
            window.clear();
            innocence = 1.0;
            return;
        }

        if (window.size() == WINDOW) window.pollFirst();
        window.addLast(horizontal);
        if (window.size() < WINDOW) return;

        double sum = 0;
        for (double v : window) sum += v;
        double avg = sum / window.size();

        double ceiling = ceilingFor(s);

        if (avg > ceiling) {
            double overageRatio = (avg - ceiling) / Math.max(ceiling, 0.05);
            setInfo("avg=" + formatOffset(avg) + " ceiling=" + formatOffset(ceiling));
            innocence = Math.max(0.0, innocence - Math.min(1.0, overageRatio) * 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }

    private double ceilingFor(WorldSnapshot s) {
        double ceiling = BASE_CEILING;
        if (s.sprinting) ceiling += 0.12;
        if (s.speedEffect) ceiling += PER_SPEED_LEVEL * (s.speedAmplifier + 1);
        if (isIce(s.belowMaterial)) ceiling += 0.30;
        return ceiling;
    }

    private boolean isExempt(WorldSnapshot s) {
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return true;
        if (s.flying || s.gliding || s.riptiding || s.insideVehicle) return true;
        if (s.dolphinsGrace || s.levitation) return true;
        Material here = s.feetMaterial;
        Material below = s.belowMaterial;
        if (here == Material.WATER || below == Material.WATER || here == Material.LAVA) return true;
        if (here == Material.BUBBLE_COLUMN || below == Material.BUBBLE_COLUMN) return true;
        return false;
    }

    private static boolean isIce(Material m) {
        return m == Material.ICE || m == Material.PACKED_ICE || m == Material.BLUE_ICE || m == Material.FROSTED_ICE;
    }
}
