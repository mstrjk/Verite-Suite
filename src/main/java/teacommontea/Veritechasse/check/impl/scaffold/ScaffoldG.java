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

package teacommontea.veritechasse.check.impl.scaffold;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "ScaffoldG", description = "Bridging faster than humanly possible.", decay = 0.0)
public final class ScaffoldG extends Check implements ConfidenceCheck {

    private static final long WINDOW_MS = 1000L;
    private static final int MAX_BRIDGE_PER_SEC = 4;

    private final Deque<Long> placements = new ArrayDeque<>();
    private double innocence = 1.0;

    public ScaffoldG(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onPlace(Player bukkit, BlockPlace place) {
        if (!ScaffoldContext.isBridging(bukkit, place)) {
            return;
        }

        long now = System.currentTimeMillis();
        placements.addLast(now);
        while (!placements.isEmpty() && now - placements.peekFirst() > WINDOW_MS) {
            placements.pollFirst();
        }

        if (placements.size() > MAX_BRIDGE_PER_SEC) {
            int over = placements.size() - MAX_BRIDGE_PER_SEC;
            setInfo("perSec=" + String.valueOf(placements.size()) + " max=" + String.valueOf(MAX_BRIDGE_PER_SEC));
            innocence = Math.max(0.0, innocence - Math.min(1.0, over / 3.0) * 0.6);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
