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

package teacommontea.veritechasse.check.impl.blink;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "BlinkA", description = "Large position jump after a packet silence (blink flush).", decay = 0.02)
public final class BlinkA extends Check implements PacketCheck, ConfidenceCheck {

    private static final long SILENCE_MS = 250L;
    private static final double JUMP_DISTANCE = 2.0;
    private static final int SUSTAIN = 2;
    private static final int IMPULSE_TICKS = 20;

    private double lastX, lastZ;
    private boolean hasLast;
    private long lastFlyingTime;
    private int blinkHits;
    private int teleportTimer;
    private double innocence = 1.0;

    public BlinkA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markTeleport() {
        teleportTimer = IMPULSE_TICKS;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (teleportTimer > 0) teleportTimer--;

        long now = System.currentTimeMillis();
        long silence = hasLast ? now - lastFlyingTime : 0;
        lastFlyingTime = now;

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
        double moved = Math.sqrt(dx * dx + dz * dz);

        boolean exempt = s.gameMode == GameMode.SPECTATOR || s.flying || s.gliding
                || s.riptiding || s.insideVehicle || teleportTimer > 0;

        if (!exempt && silence > SILENCE_MS && moved > JUMP_DISTANCE) {
            blinkHits++;
            if (blinkHits >= SUSTAIN) {
                setInfo("moved=" + formatOffset(moved) + " minJump=" + formatOffset(JUMP_DISTANCE) + " silence=" + silence);
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            blinkHits = Math.max(0, blinkHits - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
