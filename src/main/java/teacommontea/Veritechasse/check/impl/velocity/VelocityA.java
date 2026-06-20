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

package teacommontea.veritechasse.check.impl.velocity;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "VelocityA", description = "Took less horizontal knockback than applied.", decay = 0.02)
public final class VelocityA extends Check implements PacketCheck, ConfidenceCheck {

    private static final int MEASURE_TICKS = 8;
    private static final int START_DELAY_TICKS = 1;
    private static final double MIN_APPLIED = 0.1;
    private static final double REQUIRED_FRACTION = 0.33;

    private double lastX, lastZ;
    private boolean hasLast;
    private boolean measuring;
    private int measureStartTick;
    private int handledKbTick = Integer.MIN_VALUE;
    private double appliedMag;
    private double peakHorizontal;
    private double traveled;
    private boolean collidedDuringMeasure;
    private double innocence = 1.0;

    public VelocityA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;

        VeritePacketEvent flying = event;
        if (!flying.hasPositionChanged()) return;

        double x = flying.getLocation().getX();
        double z = flying.getLocation().getZ();
        if (!hasLast) {
            lastX = x;
            lastZ = z;
            hasLast = true;
            return;
        }
        double dx = x - lastX;
        double dz = z - lastZ;
        lastX = x;
        lastZ = z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle;

        if (player.kbPending && player.appliedKbTick != handledKbTick) {
            handledKbTick = player.appliedKbTick;
            double kx = player.appliedKbX;
            double kz = player.appliedKbZ;
            appliedMag = Math.sqrt(kx * kx + kz * kz);
            if (!exempt && appliedMag >= MIN_APPLIED) {
                measuring = true;
                measureStartTick = player.currentTick();
                peakHorizontal = 0.0;
                traveled = 0.0;
                collidedDuringMeasure = false;
            }
        }

        if (!measuring) return;

        if (exempt) {
            measuring = false;
            return;
        }

        if (s.horizontalCollision) collidedDuringMeasure = true;

        int elapsed = player.currentTick() - measureStartTick;

        if (elapsed >= START_DELAY_TICKS) {
            peakHorizontal = Math.max(peakHorizontal, horizontal);
            traveled += horizontal;
        }

        if (elapsed >= MEASURE_TICKS) {
            measuring = false;
            if (collidedDuringMeasure) {
                innocence = Math.min(1.0, innocence + 0.3);
                return;
            }
            double required = appliedMag * REQUIRED_FRACTION;

            double observed = Math.max(peakHorizontal, traveled);
            if (observed < required) {
                double deficit = (required - observed) / Math.max(required, 0.05);
                setInfo("peakHoriz=" + formatOffset(peakHorizontal) + " traveled=" + formatOffset(traveled)
                        + " required=" + formatOffset(required) + " appliedKb=" + formatOffset(appliedMag));
                innocence = Math.max(0.0, innocence - Math.min(1.0, deficit) * 0.5);
            } else {
                innocence = Math.min(1.0, innocence + 0.3);
            }
        }
    }
}
