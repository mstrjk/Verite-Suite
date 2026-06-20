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

package teacommontea.veritechasse.check.impl.inventory;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.VeritePlayer;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "InventoryA", description = "Inventory clicks faster than humanly possible.", decay = 0.02)
public final class InventoryA extends Check implements ConfidenceCheck {

    private static final long WINDOW_MS = 1000L;
    private static final int MAX_CLICKS_PER_WINDOW = 30;
    private static final int SUSTAIN = 2;

    private final Deque<Long> clicks = new ArrayDeque<>();
    private int fastBursts;
    private double innocence = 1.0;

    public InventoryA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onInventoryClick() {
        long now = System.currentTimeMillis();

        clicks.addLast(now);
        while (!clicks.isEmpty() && now - clicks.peekFirst() > WINDOW_MS) {
            clicks.pollFirst();
        }

        if (clicks.size() > MAX_CLICKS_PER_WINDOW) {
            fastBursts++;
            if (fastBursts >= SUSTAIN) {
                setInfo("clicks=" + clicks.size() + " max=" + MAX_CLICKS_PER_WINDOW + " windowMs=" + WINDOW_MS);
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            fastBursts = Math.max(0, fastBursts - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
