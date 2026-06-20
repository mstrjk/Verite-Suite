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

@CheckInfo(name = "VelocityB", description = "Took less vertical knockback than applied.", decay = 0.02)
public final class VelocityB extends Check implements PacketCheck, ConfidenceCheck {

    private static final int MEASURE_TICKS = 8;
    private static final int START_DELAY_TICKS = 1;
    private static final double MIN_APPLIED_Y = 0.1;
    private static final double REQUIRED_FRACTION = 0.33;

    private double lastY;
    private boolean hasLast;
    private boolean measuring;
    private int measureStartTick;
    private int handledKbTick = Integer.MIN_VALUE;
    private double appliedY;
    private double peakRise;
    private double cumulativeRise;
    private double innocence = 1.0;

    public VelocityB(VeritePlayer player) {
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

        double y = flying.getLocation().getY();
        if (!hasLast) {
            lastY = y;
            hasLast = true;
            return;
        }
        double deltaY = y - lastY;
        lastY = y;

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle;

        if (player.kbPending && player.appliedKbTick != handledKbTick) {
            handledKbTick = player.appliedKbTick;
            appliedY = player.appliedKbY;

            boolean grounded = s.bukkitOnGround || player.onGround;
            if (!exempt && !grounded && appliedY >= MIN_APPLIED_Y) {
                measuring = true;
                measureStartTick = player.currentTick();
                peakRise = 0.0;
                cumulativeRise = 0.0;
            }
        }

        if (!measuring) return;

        if (exempt) {
            measuring = false;
            return;
        }

        int elapsed = player.currentTick() - measureStartTick;

        if (elapsed >= START_DELAY_TICKS) {
            if (deltaY > peakRise) peakRise = deltaY;
            if (deltaY > 0) cumulativeRise += deltaY;
        }

        if (elapsed >= MEASURE_TICKS) {
            measuring = false;
            double required = appliedY * REQUIRED_FRACTION;
            double observed = Math.max(peakRise, cumulativeRise);
            if (observed < required) {
                double deficit = (required - observed) / Math.max(required, 0.05);
                setInfo("peakRise=" + formatOffset(peakRise) + " cumRise=" + formatOffset(cumulativeRise)
                        + " required=" + formatOffset(required) + " appliedKbY=" + formatOffset(appliedY));
                innocence = Math.max(0.0, innocence - Math.min(1.0, deficit) * 0.5);
            } else {
                innocence = Math.min(1.0, innocence + 0.3);
            }
        }
    }
}
