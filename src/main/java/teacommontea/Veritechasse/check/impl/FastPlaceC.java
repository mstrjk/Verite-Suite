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
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.BlockPlaceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;
import teacommontea.veritechasse.net.GameMode;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "FastPlaceC", description = "Hotbar discipline of a scripted printer, not a human.", decay = 0.02)
public final class FastPlaceC extends Check implements BlockPlaceCheck, PacketCheck, teacommontea.veritechasse.check.ConfidenceCheck {

    @Override
    public double innocence() {
        return innocence;
    }

    private static final int SESSION_IDLE_TICKS = 40;
    private static final int MIN_SWITCHES = 16;
    private static final double MAX_ADJACENT_FRACTION_FLOOR = 0.25;
    private static final double MIN_UNUSED_SWITCH_FRACTION = 0.05;
    private static final int SWITCH_PLACE_VARIANCE_FLOOR = 1;

    private int lastActivityTick = Integer.MIN_VALUE;
    private int lastSwitchTick = Integer.MIN_VALUE;
    private int lastSlot = Integer.MIN_VALUE;
    private boolean placedSinceSwitch = true;

    private int switches;
    private int adjacentSwitches;
    private int unusedSwitches;
    private final Deque<Integer> switchToPlaceGaps = new ArrayDeque<>();

    private double innocence = 1.0;

    public FastPlaceC(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() != VeritePacketType.HELD_ITEM_CHANGE) return;

        int tick = player.currentTick();
        maybeReset(tick);
        lastActivityTick = tick;

        int newSlot = event.getSlot();

        if (lastSlot != Integer.MIN_VALUE) {
            if (!placedSinceSwitch) unusedSwitches++;
            if (hotbarDistance(lastSlot, newSlot) <= 1) adjacentSwitches++;
            switches++;
        }

        lastSlot = newSlot;
        lastSwitchTick = tick;
        placedSinceSwitch = false;
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;
        int tick = player.currentTick();
        maybeReset(tick);
        lastActivityTick = tick;

        if (!placedSinceSwitch && lastSwitchTick != Integer.MIN_VALUE) {
            if (switchToPlaceGaps.size() == 64) switchToPlaceGaps.pollFirst();
            switchToPlaceGaps.addLast(tick - lastSwitchTick);
        }
        placedSinceSwitch = true;

        if (switches < MIN_SWITCHES) return;

        double adjacentFraction = (double) adjacentSwitches / switches;
        double unusedFraction = (double) unusedSwitches / switches;
        double gapVariance = variance(switchToPlaceGaps);

        boolean directJumps = adjacentFraction < MAX_ADJACENT_FRACTION_FLOOR;
        boolean noFumbles = unusedFraction < MIN_UNUSED_SWITCH_FRACTION;
        boolean roboticTiming = switchToPlaceGaps.size() >= 8 && gapVariance < SWITCH_PLACE_VARIANCE_FLOOR;

        int votes = (directJumps ? 1 : 0) + (noFumbles ? 1 : 0) + (roboticTiming ? 1 : 0);
        if (votes >= 2) {
            setInfo("votes=" + String.valueOf(votes) + " adjacentFrac=" + formatOffset(adjacentFraction) + " unusedFrac=" + formatOffset(unusedFraction) + " gapVar=" + formatOffset(gapVariance));
            innocence = Math.max(0.0, innocence - 0.25 * votes);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }

    private void maybeReset(int tick) {
        if (lastActivityTick != Integer.MIN_VALUE && tick - lastActivityTick > SESSION_IDLE_TICKS) {
            lastSwitchTick = Integer.MIN_VALUE;
            lastSlot = Integer.MIN_VALUE;
            placedSinceSwitch = true;
            switches = 0;
            adjacentSwitches = 0;
            unusedSwitches = 0;
            switchToPlaceGaps.clear();
        }
    }

    private static int hotbarDistance(int a, int b) {
        int direct = Math.abs(a - b);
        return Math.min(direct, 9 - direct);
    }

    private static double variance(Deque<Integer> values) {
        if (values.isEmpty()) return Double.MAX_VALUE;
        double mean = 0;
        for (int v : values) mean += v;
        mean /= values.size();
        double sum = 0;
        for (int v : values) { double d = v - mean; sum += d * d; }
        return sum / values.size();
    }
}
