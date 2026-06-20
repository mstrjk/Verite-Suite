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

package teacommontea.veritechasse.check.impl.reach;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.entity.TrackedEntity;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "ReachA", description = "Attacked an entity from beyond reach.", decay = 0.02)
public final class ReachA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double VANILLA_REACH = 3.0;
    private static final double TOLERANCE = 0.1;
    private static final int LAG_SAMPLE_TICKS = 3;
    private static final double EYE_HEIGHT = 1.62;
    private static final int SUSTAIN = 2;

    private int overHits;
    private double innocence = 1.0;

    public ReachA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() != VeritePacketType.ATTACK) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.insideVehicle) {
            return;
        }

        TrackedEntity target = player.entityTracker.get(event.getInteractEntityId());
        if (target == null) return;

        double minDist = Double.MAX_VALUE;
        for (int t = 0; t <= LAG_SAMPLE_TICKS; t++) {
            double tx = target.histXAgo(t);
            double ty = target.histYAgo(t);
            double tz = target.histZAgo(t);
            double d = distanceToBox(player.x, player.y + EYE_HEIGHT, player.z,
                    tx, ty, tz, target.width, target.height);
            if (d < minDist) minDist = d;
        }

        if (minDist > VANILLA_REACH + TOLERANCE) {
            overHits++;
            if (overHits >= SUSTAIN) {
                double over = minDist - (VANILLA_REACH + TOLERANCE);
                setInfo("dist=" + formatOffset(minDist) + " maxReach=" + formatOffset(VANILLA_REACH + TOLERANCE) + " over=" + formatOffset(over));
                innocence = Math.max(0.0, innocence - Math.min(1.0, over) * 0.6);
            }
        } else {
            overHits = Math.max(0, overHits - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }

    private static double distanceToBox(double px, double py, double pz,
                                        double ex, double ey, double ez,
                                        double width, double height) {
        double half = width / 2.0;
        double minX = ex - half, maxX = ex + half;
        double minY = ey, maxY = ey + height;
        double minZ = ez - half, maxZ = ez + half;

        double dx = Math.max(Math.max(minX - px, 0.0), px - maxX);
        double dy = Math.max(Math.max(minY - py, 0.0), py - maxY);
        double dz = Math.max(Math.max(minZ - pz, 0.0), pz - maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
