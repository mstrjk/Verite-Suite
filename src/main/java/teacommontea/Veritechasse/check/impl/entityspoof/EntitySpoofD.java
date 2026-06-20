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

@CheckInfo(name = "EntitySpoofD", description = "Mount moving faster than its maximum speed.", decay = 0.0)
public final class EntitySpoofD extends Check implements ConfidenceCheck {

    private static final double SPEED_TO_BPS = 43.17;
    private static final double TOLERANCE = 1.35;
    private static final int SUSTAIN = 3;

    private double lastX, lastZ;
    private boolean hasLast;
    private long lastTime;
    private int overTicks;
    private double innocence = 1.0;

    public EntitySpoofD(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void onVehicleMove(Player bukkit, double boatX, double boatZ) {
        WorldSnapshot.VehicleSnapshot v = player.vehicle;
        if (!v.present || !v.living || !v.vanillaRideable) {
            innocence = 1.0;
            hasLast = false;
            overTicks = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (!hasLast) {
            lastX = boatX;
            lastZ = boatZ;
            lastTime = now;
            hasLast = true;
            return;
        }
        double dx = boatX - lastX;
        double dz = boatZ - lastZ;
        double dt = Math.max(1, now - lastTime) / 1000.0;
        lastX = boatX;
        lastZ = boatZ;
        lastTime = now;

        double bps = Math.sqrt(dx * dx + dz * dz) / dt;
        double maxBps = v.mountSpeedAttr * SPEED_TO_BPS;

        if (bps > maxBps * TOLERANCE) {
            overTicks++;
            if (overTicks >= SUSTAIN) {
                setInfo("bps=" + String.format("%.3f", bps) + " max=" + String.format("%.3f", maxBps * TOLERANCE) + " ticks=" + String.valueOf(overTicks));
                innocence = Math.max(0.0, innocence - 0.4);
            }
        } else {
            overTicks = 0;
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
