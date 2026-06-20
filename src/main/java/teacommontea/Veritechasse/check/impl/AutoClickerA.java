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
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "AutoClickerA", description = "Attacked multiple entities within one tick window.", decay = 0.01)
public final class AutoClickerA extends Check implements PacketCheck, ConfidenceCheck {

    private static final long WINDOW_MS = 60L;

    private int lastEntityId = Integer.MIN_VALUE;
    private long lastAttackMs = Long.MIN_VALUE;
    private double innocence = 1.0;

    public AutoClickerA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() != VeritePacketType.ATTACK) return;

        int id = event.getInteractEntityId();
        long now = System.currentTimeMillis();

        if (lastAttackMs != Long.MIN_VALUE && now - lastAttackMs <= WINDOW_MS && id != lastEntityId) {
            setInfo("gap=" + String.valueOf(now - lastAttackMs) + "ms window=" + String.valueOf(WINDOW_MS) + "ms");
            innocence = Math.max(0.0, innocence - 0.5);
        } else {
            innocence = Math.min(1.0, innocence + 0.1);
        }

        lastEntityId = id;
        lastAttackMs = now;
    }
}
