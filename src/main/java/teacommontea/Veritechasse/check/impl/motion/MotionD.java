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

package teacommontea.veritechasse.check.impl.motion;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "MotionD", description = "Horizontal speed is a fabricated clean multiple.", decay = 0.02)
public final class MotionD extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MODULO = 0.1;
    private static final double PRECISION = 1.0E-5;
    private static final double MIN_SPEED = 0.11;
    private static final double MAX_SPEED = 0.25;
    private static final int SUSTAIN = 4;

    private double lastX, lastZ;
    private boolean hasLast;
    private int cleanTicks;
    private int teleportTimer;
    private int velocityTimer;
    private double innocence = 1.0;

    public MotionD(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markTeleport() {
        teleportTimer = 10;
    }

    public void markVelocity() {
        velocityTimer = 10;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;

        if (teleportTimer > 0) teleportTimer--;
        if (velocityTimer > 0) velocityTimer--;

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
        double speed = Math.sqrt(dx * dx + dz * dz);

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle
                || teleportTimer > 0 || velocityTimer > 0;

        if (exempt || speed < MIN_SPEED || speed > MAX_SPEED) {
            cleanTicks = Math.max(0, cleanTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
            return;
        }

        double rem = speed % MODULO;
        boolean clean = rem < PRECISION || (MODULO - rem) < PRECISION;

        if (clean) {
            cleanTicks++;
            if (cleanTicks >= SUSTAIN) {
                setInfo("speed=" + formatOffset(speed) + " rem=" + formatOffset(rem) + " cleanTicks=" + cleanTicks);
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            cleanTicks = Math.max(0, cleanTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
