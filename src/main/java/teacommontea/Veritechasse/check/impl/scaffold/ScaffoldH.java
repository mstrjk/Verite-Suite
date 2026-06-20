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

@CheckInfo(name = "ScaffoldH", description = "Bridging while not looking down at the placement.", decay = 0.0)
public final class ScaffoldH extends Check implements ConfidenceCheck {

    private static final float MIN_DOWN_PITCH = 20.0f;
    private static final int SUSTAIN = 3;

    private int badPlaces;
    private double innocence = 1.0;

    public ScaffoldH(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onPlace(Player bukkit, BlockPlace place) {
        if (!ScaffoldContext.isBridging(bukkit, place)) {
            innocence = 1.0;
            badPlaces = 0;
            return;
        }

        float pitch = player.pitch;
        if (pitch < MIN_DOWN_PITCH) {
            badPlaces++;
            if (badPlaces >= SUSTAIN) {
                setInfo("pitch=" + formatOffset(pitch) + " minDown=" + formatOffset(MIN_DOWN_PITCH));
                innocence = Math.max(0.0, innocence - 0.4);
            }
        } else {
            badPlaces = Math.max(0, badPlaces - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
