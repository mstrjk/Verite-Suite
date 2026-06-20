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

package teacommontea.veritechasse.check.impl.badpackets;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.net.VeritePacketEvent.DiggingAction;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "BadPacketsOffhandCrash", description = "Flooding offhand-swap action packets (crash exploit).")
public final class BadPacketOffhandCrash extends Check implements PacketCheck {

    private static final long WINDOW_MS = 1000L;
    private static final int MAX_SWAPS_PER_WINDOW = 12;

    private long windowStart;
    private int swaps;

    public BadPacketOffhandCrash(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() != VeritePacketType.PLAYER_DIGGING) return;

        if (event.getDiggingAction() != DiggingAction.SWAP_ITEM_WITH_OFFHAND) return;

        long now = System.currentTimeMillis();
        if (now - windowStart > WINDOW_MS) {
            windowStart = now;
            swaps = 0;
        }
        swaps++;
        if (swaps > MAX_SWAPS_PER_WINDOW) {
            flag("swaps/s=" + swaps);
            swaps = 0;
        }
    }
}
