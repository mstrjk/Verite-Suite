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

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "FastBowB", description = "Fired bow launches too close together.", decay = 0.05)
public final class FastBowB extends Check implements ConfidenceCheck {

    private static final int MIN_SPACING_TICKS = 3;

    private int lastLaunchTick = Integer.MIN_VALUE;
    private double innocence = 1.0;

    public FastBowB(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onBowShoot(int currentTick) {
        if (lastLaunchTick != Integer.MIN_VALUE) {
            int spacing = currentTick - lastLaunchTick;
            if (spacing < MIN_SPACING_TICKS) {
                setInfo("spacing=" + String.valueOf(spacing) + " minSpacing=" + String.valueOf(MIN_SPACING_TICKS));
                innocence = Math.max(0.0, innocence - 0.6);
            } else {
                innocence = Math.min(1.0, innocence + 0.2);
            }
        }
        lastLaunchTick = currentTick;
    }
}
