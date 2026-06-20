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

package teacommontea.veritechasse.check.impl.fastuse;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.net.VeritePacketEvent.DiggingAction;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "FastUseA", description = "Finishing item use faster than vanilla allows.", decay = 0.02)
public final class FastUseA extends Check implements PacketCheck, ConfidenceCheck {

    private static final int MIN_USE_TICKS = 4;
    private static final int SUSTAIN = 3;

    private boolean using;
    private int useStartTick;
    private int tick;
    private int fastReleases;
    private double innocence = 1.0;

    public FastUseA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.isFlying()) {
            tick++;
            return;
        }

        if (event.getPacketType() == VeritePacketType.USE_ITEM) {
            using = true;
            useStartTick = tick;
            return;
        }

        if (event.getPacketType() != VeritePacketType.PLAYER_DIGGING) return;
        if (event.getDiggingAction() != DiggingAction.RELEASE_USE_ITEM) return;
        if (!using) return;

        int useTicks = tick - useStartTick;
        using = false;

        if (useTicks < MIN_USE_TICKS) {
            fastReleases++;
            if (fastReleases >= SUSTAIN) {
                setInfo("useTicks=" + String.valueOf(useTicks) + " min=" + String.valueOf(MIN_USE_TICKS));
                innocence = Math.max(0.0, innocence - 0.4);
            }
        } else {
            fastReleases = Math.max(0, fastReleases - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
