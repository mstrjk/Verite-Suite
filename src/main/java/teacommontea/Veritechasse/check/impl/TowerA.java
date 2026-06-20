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
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "TowerA", description = "Towered too quickly.", decay = 0.05)
public final class TowerA extends Check implements BlockPlaceCheck, teacommontea.veritechasse.check.ConfidenceCheck {

    @Override
    public double innocence() {
        return innocence;
    }

    private static final int MIN_SPACING_TICKS = 2;

    private int lastTick = Integer.MIN_VALUE;
    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private double innocence = 1.0;

    public TowerA(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE) return;

        Vec3i pos = place.position;
        int currentTick = player.currentTick();

        boolean sameColumn = pos.getX() == lastX && pos.getZ() == lastZ;
        int spacing = currentTick - lastTick;

        if (sameColumn && lastTick != Integer.MIN_VALUE && spacing < MIN_SPACING_TICKS) {
            setInfo("spacing=" + spacing + " minSpacing=" + MIN_SPACING_TICKS);
            innocence = Math.max(0.0, innocence - 0.34);
        } else {
            innocence = Math.min(1.0, innocence + 0.2);
        }

        lastX = pos.getX();
        lastZ = pos.getZ();
        lastTick = currentTick;
    }
}
