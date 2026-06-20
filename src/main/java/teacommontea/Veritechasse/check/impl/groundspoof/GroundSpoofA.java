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

package teacommontea.veritechasse.check.impl.groundspoof;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.GroundDetector;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "GroundSpoofA", description = "Claiming onGround while falling through air (NoFall).", decay = 0.02)
public final class GroundSpoofA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double FALL_THRESHOLD = -0.5;
    private static final double DESCENT_THRESHOLD = -0.08;
    private static final double RISE_THRESHOLD = 0.1;
    private static final int SUSTAIN = 2;
    private static final int DESCENT_SUSTAIN = 4;
    private static final int IMPULSE_TICKS = 10;

    private double lastY;
    private boolean hasLast;
    private int spoofTicks;
    private int descentSpoofTicks;
    private int teleportTimer;
    private double innocence = 1.0;

    public GroundSpoofA(VeritePlayer player) {
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

        VeritePacketEvent flying = event;
        if (!flying.hasPositionChanged()) return;

        double x = flying.getLocation().getX();
        double y = flying.getLocation().getY();
        double z = flying.getLocation().getZ();
        boolean claimsGround = flying.isOnGround();

        if (!hasLast) {
            lastY = y;
            hasLast = true;
            return;
        }
        double deltaY = y - lastY;
        lastY = y;

        if (isExempt(s) || teleportTimer > 0) {
            spoofTicks = 0;
            descentSpoofTicks = 0;
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        boolean reallyGrounded = GroundDetector.isOnGround(s, x, z, y);
        boolean spoofing = claimsGround && !reallyGrounded;
        boolean fallingFast = deltaY <= FALL_THRESHOLD;
        boolean descending = deltaY <= DESCENT_THRESHOLD;
        boolean rising = deltaY >= RISE_THRESHOLD;

        if (spoofing && (fallingFast || rising)) {
            spoofTicks++;
            descentSpoofTicks = 0;
            if (spoofTicks >= SUSTAIN) {
                setInfo("claimsGround=" + claimsGround + " reallyGrounded=" + reallyGrounded + " deltaY=" + formatOffset(deltaY) + " ticks=" + String.valueOf(spoofTicks));
                innocence = Math.max(0.0, innocence - 0.6);
            }
        } else if (spoofing && descending) {
            descentSpoofTicks++;
            spoofTicks = Math.max(0, spoofTicks - 1);
            if (descentSpoofTicks >= DESCENT_SUSTAIN) {
                setInfo("claimsGround=" + claimsGround + " reallyGrounded=" + reallyGrounded + " deltaY=" + formatOffset(deltaY) + " descentTicks=" + String.valueOf(descentSpoofTicks));
                innocence = Math.max(0.0, innocence - 0.4);
            }
        } else {
            spoofTicks = Math.max(0, spoofTicks - 1);
            descentSpoofTicks = Math.max(0, descentSpoofTicks - 1);
            innocence = Math.min(1.0, innocence + 0.25);
        }
    }

    private boolean isExempt(WorldSnapshot s) {
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR) return true;
        if (s.flying || s.gliding || s.riptiding || s.insideVehicle) return true;
        if (s.levitation || s.slowFalling) return true;
        return false;
    }
}
