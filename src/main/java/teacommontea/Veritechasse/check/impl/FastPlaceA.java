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

import teacommontea.veritechasse.net.GameMode;
import teacommontea.veritechasse.net.Vec3i;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.BlockPlaceCheck;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "FastPlaceA", description = "Placed blocks too quickly.", decay = 0.05)
public final class FastPlaceA extends Check implements BlockPlaceCheck, ConfidenceCheck {

    private static final int RATE_WINDOW_TICKS = 20;
    private static final int MAX_PLACEMENTS_PER_WINDOW = 18;
    private static final int MIN_SPACING_TICKS = 1;

    private int lastPlaceTick = Integer.MIN_VALUE;
    private int lastX, lastY, lastZ;
    private final Deque<Integer> recent = new ArrayDeque<>();
    private double innocence = 1.0;

    public FastPlaceA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE) return;

        int tick = player.currentTick();
        Vec3i p = place.position;

        recent.addLast(tick);
        while (!recent.isEmpty() && tick - recent.peekFirst() > RATE_WINDOW_TICKS) {
            recent.pollFirst();
        }

        boolean fast = false;

        if (recent.size() > MAX_PLACEMENTS_PER_WINDOW) {
            fast = true;
        }

        if (lastPlaceTick != Integer.MIN_VALUE) {
            int spacing = tick - lastPlaceTick;
            int leap = Math.abs(p.getX() - lastX) + Math.abs(p.getY() - lastY) + Math.abs(p.getZ() - lastZ);
            if (spacing <= MIN_SPACING_TICKS && leap > 1) {
                fast = true;
            }
        }

        if (fast) {
            setInfo("placesPerWindow=" + String.valueOf(recent.size()) + " max=" + String.valueOf(MAX_PLACEMENTS_PER_WINDOW));
            innocence = Math.max(0.0, innocence - 0.34);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }

        lastPlaceTick = tick;
        lastX = p.getX();
        lastY = p.getY();
        lastZ = p.getZ();
    }
}
