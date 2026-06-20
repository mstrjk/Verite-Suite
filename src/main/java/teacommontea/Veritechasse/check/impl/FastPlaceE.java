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

import teacommontea.veritechasse.net.Vec3i;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.BlockPlaceCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;
import teacommontea.veritechasse.net.GameMode;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "FastPlaceE", description = "Build trajectory inconsistent with a human (15s window).", decay = 0.02)
public final class FastPlaceE extends Check implements BlockPlaceCheck, teacommontea.veritechasse.check.ConfidenceCheck {

    @Override
    public double innocence() {
        return innocence;
    }

    private static final int WINDOW_TICKS = 300;
    private static final int MIN_SAMPLE = 12;
    private static final double MAX_MEAN_REACH = 6.5;
    private static final double MAX_SPREAD_PER_TRAVEL = 14.0;
    private static final double MIN_ADJACENT_FRACTION = 0.15;

    private final Deque<Sample> window = new ArrayDeque<>();
    private double innocence = 1.0;

    public FastPlaceE(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;
        int tick = player.currentTick();
        Vec3i p = place.position;

        window.addLast(new Sample(tick, p.getX(), p.getY(), p.getZ(), player.x, player.y, player.z));
        while (!window.isEmpty() && tick - window.peekFirst().tick > WINDOW_TICKS) {
            window.pollFirst();
        }
        if (window.size() < MIN_SAMPLE) return;

        double sumReach = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        double playerMinX = Double.MAX_VALUE, playerMinZ = Double.MAX_VALUE;
        double playerMaxX = -Double.MAX_VALUE, playerMaxZ = -Double.MAX_VALUE;
        int adjacent = 0;

        Sample prev = null;
        for (Sample s : window) {
            double dx = s.bx + 0.5 - s.px;
            double dy = s.by + 0.5 - s.py;
            double dz = s.bz + 0.5 - s.pz;
            sumReach += Math.sqrt(dx * dx + dy * dy + dz * dz);

            minX = Math.min(minX, s.bx); maxX = Math.max(maxX, s.bx);
            minY = Math.min(minY, s.by); maxY = Math.max(maxY, s.by);
            minZ = Math.min(minZ, s.bz); maxZ = Math.max(maxZ, s.bz);

            playerMinX = Math.min(playerMinX, s.px); playerMaxX = Math.max(playerMaxX, s.px);
            playerMinZ = Math.min(playerMinZ, s.pz); playerMaxZ = Math.max(playerMaxZ, s.pz);

            if (prev != null && isAdjacent(prev, s)) adjacent++;
            prev = s;
        }

        int n = window.size();
        double meanReach = sumReach / n;

        double spanDiag = Math.sqrt(sq(maxX - minX) + sq(maxY - minY) + sq(maxZ - minZ));
        double playerTravel = Math.sqrt(sq(playerMaxX - playerMinX) + sq(playerMaxZ - playerMinZ));
        double spreadPerTravel = spanDiag / Math.max(0.5, playerTravel);

        double adjacentFraction = (double) adjacent / (n - 1);

        boolean reachBad = meanReach > MAX_MEAN_REACH;
        boolean spreadBad = spreadPerTravel > MAX_SPREAD_PER_TRAVEL;
        boolean contiguityBad = adjacentFraction < MIN_ADJACENT_FRACTION;

        int votes = (reachBad ? 1 : 0) + (spreadBad ? 1 : 0) + (contiguityBad ? 1 : 0);
        if (votes >= 2) {
            setInfo("votes=" + String.valueOf(votes) + " meanReach=" + formatOffset(meanReach) + " spreadPerTravel=" + formatOffset(spreadPerTravel) + " adjacentFrac=" + formatOffset(adjacentFraction));
            innocence = Math.max(0.0, innocence - 0.25 * votes);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }

    private static boolean isAdjacent(Sample a, Sample b) {
        return Math.abs(a.bx - b.bx) <= 1 && Math.abs(a.by - b.by) <= 1 && Math.abs(a.bz - b.bz) <= 1;
    }

    private static double sq(double v) {
        return v * v;
    }

    private record Sample(int tick, int bx, int by, int bz, double px, double py, double pz) {
    }
}
