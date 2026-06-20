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

package teacommontea.veritechasse.check.impl.boatfly;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.entity.Player;

@CheckInfo(name = "BoatFlyC", description = "Airborne boat travelling horizontally.", decay = 0.0)
public final class BoatFlyC extends Check implements ConfidenceCheck {

    private static final double MOVE_THRESHOLD = 0.08;
    private static final int SUSTAIN = 3;

    private double lastX, lastZ;
    private boolean hasLast;
    private int airborneMovingTicks;
    private double innocence = 1.0;

    public BoatFlyC(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onVehicleMove(Player bukkit, double boatX, double boatZ) {
        WorldSnapshot.VehicleSnapshot v = player.vehicle;
        if (!BoatFlyContext.active(v)) {
            innocence = 1.0;
            airborneMovingTicks = 0;
            hasLast = false;
            return;
        }

        if (!hasLast) {
            lastX = boatX;
            lastZ = boatZ;
            hasLast = true;
            return;
        }
        double dx = boatX - lastX;
        double dz = boatZ - lastZ;
        lastX = boatX;
        lastZ = boatZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (BoatFlyContext.unsupported(v) && horizontal > MOVE_THRESHOLD) {
            airborneMovingTicks++;
            if (airborneMovingTicks >= SUSTAIN) {
                setInfo("horizontal=" + formatOffset(horizontal) + " moveThreshold=" + formatOffset(MOVE_THRESHOLD) + " movingTicks=" + airborneMovingTicks);
                innocence = Math.max(0.0, innocence - 0.4);
            }
        } else {
            airborneMovingTicks = 0;
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
