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

package teacommontea.veritechasse.check.impl.entityspoof;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.entity.Player;

@CheckInfo(name = "EntitySpoofG", description = "Mount flying (non Happy Ghast).", decay = 0.0)
public final class EntitySpoofG extends Check implements ConfidenceCheck {

    private static final double RISE_THRESHOLD = 0.02;
    private static final int SUSTAIN = 3;

    private double lastY;
    private boolean hasLast;
    private int airborneRiseTicks;
    private double innocence = 1.0;

    public EntitySpoofG(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onVehicleMove(Player bukkit, double mountY) {
        WorldSnapshot.VehicleSnapshot v = player.vehicle;
        if (!v.present || v.flyer) {
            innocence = 1.0;
            hasLast = false;
            airborneRiseTicks = 0;
            return;
        }

        if (!hasLast) {
            lastY = mountY;
            hasLast = true;
            return;
        }
        double deltaY = mountY - lastY;
        lastY = mountY;

        boolean airborne = !v.solidUnder && !v.inWater;

        if (airborne && deltaY > RISE_THRESHOLD) {
            airborneRiseTicks++;
            if (airborneRiseTicks >= SUSTAIN) {
                setInfo("deltaY=" + formatOffset(deltaY) + " riseThreshold=" + formatOffset(RISE_THRESHOLD) + " ticks=" + String.valueOf(airborneRiseTicks) + " flyer=" + v.flyer);
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            airborneRiseTicks = 0;
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
