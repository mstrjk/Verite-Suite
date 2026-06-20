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

package teacommontea.veritechasse.check.impl.noslow;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "NoSlowB", description = "Moving too fast while using an item.", decay = 0.0)
public final class NoSlowB extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MAX_USE_SPEED = 0.13;
    private static final int USE_GRACE_TICKS = 8;
    private static final int SUSTAIN = 4;
    private static final int RAISED_WINDOW = 12;
    private static final int RAISED_MIN = 9;

    private double lastX, lastZ;
    private boolean hasLast;
    private int overTicks;
    private final boolean[] raisedHistory = new boolean[RAISED_WINDOW];
    private int raisedIndex;
    private double innocence = 1.0;

    public NoSlowB(VeritePlayer player) {
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

        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding) {
            innocence = 1.0;
            overTicks = 0;
            return;
        }

        if (!player.onGround) {
            innocence = Math.min(1.0, innocence + 0.2);
            overTicks = 0;
            return;
        }

        raisedHistory[raisedIndex] = s.handRaised;
        raisedIndex = (raisedIndex + 1) % RAISED_WINDOW;
        int raisedCount = 0;
        for (boolean r : raisedHistory) if (r) raisedCount++;

        boolean predominantlyRaised = raisedCount >= RAISED_MIN;

        if (!s.handRaised && !predominantlyRaised) {
            innocence = Math.min(1.0, innocence + 0.2);
            overTicks = 0;
            return;
        }

        if (s.handRaisedTime < USE_GRACE_TICKS && !predominantlyRaised) {
            return;
        }

        if (horizontal > MAX_USE_SPEED) {
            overTicks++;
            if (overTicks >= SUSTAIN) {
                setInfo("horizontal=" + formatOffset(horizontal) + " maxUseSpeed=" + formatOffset(MAX_USE_SPEED) + " raisedCount=" + String.valueOf(raisedCount));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            overTicks = 0;
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
