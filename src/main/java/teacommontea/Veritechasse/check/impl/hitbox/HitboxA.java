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

package teacommontea.veritechasse.check.impl.hitbox;

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

@CheckInfo(name = "HitboxA", description = "Attack ray does not intersect the target hitbox.", decay = 0.02)
public final class HitboxA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double EYE_HEIGHT = 1.62;
    private static final double REACH = 3.1;
    private static final double LAG_EXPAND = 0.3;
    private static final int LAG_SAMPLE_TICKS = 3;
    private static final int SUSTAIN = 2;

    private int overHits;
    private double innocence = 1.0;

    public HitboxA(VeritePlayer player) {
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
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return;

        TrackedEntity target = player.entityTracker.get(event.getInteractEntityId());
        if (target == null) return;

        double ox = player.x;
        double oy = player.y + EYE_HEIGHT;
        double oz = player.z;

        double yawRad = Math.toRadians(player.yaw);
        double pitchRad = Math.toRadians(player.pitch);
        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);
        double dz = Math.cos(yawRad) * Math.cos(pitchRad);

        boolean anyHit = false;
        for (int t = 0; t <= LAG_SAMPLE_TICKS && !anyHit; t++) {
            double ex = target.histXAgo(t);
            double ey = target.histYAgo(t);
            double ez = target.histZAgo(t);
            double half = target.width / 2.0 + LAG_EXPAND;
            double minX = ex - half, maxX = ex + half;
            double minY = ey - LAG_EXPAND, maxY = ey + target.height + LAG_EXPAND;
            double minZ = ez - half, maxZ = ez + half;
            if (rayIntersectsBox(ox, oy, oz, dx, dy, dz, minX, minY, minZ, maxX, maxY, maxZ)) {
                anyHit = true;
            }
        }

        if (!anyHit) {
            overHits++;
            if (overHits >= SUSTAIN) {
                setInfo("rayMiss=true reach=" + formatOffset(REACH) + " misses=" + String.valueOf(overHits));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            overHits = Math.max(0, overHits - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }

    private static boolean rayIntersectsBox(double ox, double oy, double oz,
                                            double dx, double dy, double dz,
                                            double minX, double minY, double minZ,
                                            double maxX, double maxY, double maxZ) {
        double tMin = 0.0;
        double tMax = REACH;

        double[] o = {ox, oy, oz};
        double[] d = {dx, dy, dz};
        double[] lo = {minX, minY, minZ};
        double[] hi = {maxX, maxY, maxZ};

        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-8) {
                if (o[i] < lo[i] || o[i] > hi[i]) return false;
            } else {
                double inv = 1.0 / d[i];
                double t1 = (lo[i] - o[i]) * inv;
                double t2 = (hi[i] - o[i]) * inv;
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) return false;
            }
        }
        return true;
    }
}
