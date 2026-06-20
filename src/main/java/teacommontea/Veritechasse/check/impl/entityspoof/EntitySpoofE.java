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

@CheckInfo(name = "EntitySpoofE", description = "Mount jump exceeds the animal's possible range.", decay = 0.0)
public final class EntitySpoofE extends Check implements ConfidenceCheck {

    private static final double EPSILON = 0.15;

    private double peakRiseFromGround;
    private double groundY;
    private boolean ascending;
    private double lastY;
    private boolean hasLast;
    private double innocence = 1.0;

    public EntitySpoofE(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onVehicleMove(Player bukkit, double mountY) {
        WorldSnapshot.VehicleSnapshot v = player.vehicle;
        if (!v.present || !v.horse) {
            innocence = 1.0;
            hasLast = false;
            return;
        }

        if (!hasLast) {
            lastY = mountY;
            groundY = mountY;
            hasLast = true;
            return;
        }
        double deltaY = mountY - lastY;
        lastY = mountY;

        if (deltaY > 0 && !ascending) {

            ascending = true;
            groundY = mountY - deltaY;
            peakRiseFromGround = 0;
        }
        if (ascending) {
            peakRiseFromGround = Math.max(peakRiseFromGround, mountY - groundY);
            if (deltaY <= 0) {

                ascending = false;
                double maxJump = maxJumpHeight(v.horseJumpStrength);
                if (peakRiseFromGround > maxJump + EPSILON) {
                    setInfo("peakRise=" + String.format("%.3f", peakRiseFromGround) + " maxJump=" + String.format("%.3f", maxJump + EPSILON) + " jumpStrength=" + String.format("%.3f", v.horseJumpStrength));
                    innocence = Math.max(0.0, innocence - 0.5);
                } else {
                    innocence = Math.min(1.0, innocence + 0.3);
                }
            }
        }
    }

    private static double maxJumpHeight(double j) {
        return -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367;
    }
}
