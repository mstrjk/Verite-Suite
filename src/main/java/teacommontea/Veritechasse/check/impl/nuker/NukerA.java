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

package teacommontea.veritechasse.check.impl.nuker;

import teacommontea.veritechasse.check.BlockBreakCheck;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.BlockBreak;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "NukerA", description = "Broke too many blocks in a short window.", decay = 0.05)
public final class NukerA extends Check implements BlockBreakCheck, ConfidenceCheck {

    private static final long WINDOW_MS = 1000L;
    private static final int MAX_BREAKS_PER_WINDOW = 8;

    private final Deque<Long> breaks = new ArrayDeque<>();
    private double innocence = 1.0;

    public NukerA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onBlockBreak(final BlockBreak blockBreak) {
        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return;

        long now = System.currentTimeMillis();
        breaks.addLast(now);
        while (!breaks.isEmpty() && now - breaks.peekFirst() > WINDOW_MS) {
            breaks.pollFirst();
        }

        if (breaks.size() > MAX_BREAKS_PER_WINDOW) {
            int over = breaks.size() - MAX_BREAKS_PER_WINDOW;
            setInfo("breaks=" + String.valueOf(breaks.size()) + " max=" + String.valueOf(MAX_BREAKS_PER_WINDOW) + " windowMs=" + String.valueOf(WINDOW_MS));
            innocence = Math.max(0.0, innocence - Math.min(1.0, over / 6.0) * 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
