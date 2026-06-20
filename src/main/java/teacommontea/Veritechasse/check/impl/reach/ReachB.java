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

@CheckInfo(name = "ReachB", description = "Attack only valid against a stale target position (backtrack).", decay = 0.02)
public final class ReachB extends Check implements PacketCheck, ConfidenceCheck {

    private static final double VANILLA_REACH = 3.0;
    private static final double TOLERANCE = 0.15;
    private static final double EYE_HEIGHT = 1.62;
    private static final int STALE_FROM = 4;
    private static final int STALE_TO = 10;
    private static final int SUSTAIN = 2;

    private int backtrackHits;
    private double innocence = 1.0;

    public ReachB(VeritePlayer player) {
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

        double ex = player.x;
        double ey = player.y + EYE_HEIGHT;
        double ez = player.z;

        double recentMin = Double.MAX_VALUE;
        for (int t = 0; t <= STALE_FROM - 1; t++) {
            recentMin = Math.min(recentMin, distanceToBox(ex, ey, ez,
                    target.histXAgo(t), target.histYAgo(t), target.histZAgo(t),
                    target.width, target.height));
        }

        double staleMin = Double.MAX_VALUE;
        for (int t = STALE_FROM; t <= STALE_TO; t++) {
            staleMin = Math.min(staleMin, distanceToBox(ex, ey, ez,
                    target.histXAgo(t), target.histYAgo(t), target.histZAgo(t),
                    target.width, target.height));
        }

        boolean recentInvalid = recentMin > VANILLA_REACH + TOLERANCE;
        boolean staleValid = staleMin <= VANILLA_REACH + TOLERANCE;

        if (recentInvalid && staleValid) {
            backtrackHits++;
            if (backtrackHits >= SUSTAIN) {
                setInfo("recentDist=" + formatOffset(recentMin) + " staleDist=" + formatOffset(staleMin) + " maxReach=" + formatOffset(VANILLA_REACH + TOLERANCE));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            backtrackHits = Math.max(0, backtrackHits - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }

    private static double distanceToBox(double px, double py, double pz,
                                        double ex, double ey, double ez,
                                        double width, double height) {
        double half = width / 2.0;
        double dx = Math.max(Math.max((ex - half) - px, 0.0), px - (ex + half));
        double dy = Math.max(Math.max(ey - py, 0.0), py - (ey + height));
        double dz = Math.max(Math.max((ez - half) - pz, 0.0), pz - (ez + half));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
