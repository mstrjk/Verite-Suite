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

package teacommontea.veritechasse.check.impl.step;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "StepA", description = "Stepped up more than a vanilla step in one tick.", decay = 0.02)
public final class StepA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double VANILLA_STEP = 0.6;
    private static final double TOLERANCE = 0.03;
    private static final int IMPULSE_TICKS = 10;

    private double lastY;
    private boolean hasLast;
    private boolean wasOnGround = true;
    private int teleportTimer;
    private double innocence = 1.0;

    public StepA(VeritePlayer player) {
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

        double y = flying.getLocation().getY();
        boolean onGround = flying.isOnGround();

        if (!hasLast) {
            lastY = y;
            hasLast = true;
            wasOnGround = onGround;
            return;
        }
        double deltaY = y - lastY;
        lastY = y;

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle
                || s.levitation || teleportTimer > 0;

        boolean groundToGround = wasOnGround && onGround;
        wasOnGround = onGround;

        if (exempt || !groundToGround) {
            innocence = Math.min(1.0, innocence + 0.25);
            return;
        }

        if (deltaY > VANILLA_STEP + TOLERANCE) {
            setInfo("step=" + formatOffset(deltaY) + " max=" + formatOffset(VANILLA_STEP + TOLERANCE));
            innocence = Math.max(0.0, innocence - 0.6);
        } else {
            innocence = Math.min(1.0, innocence + 0.25);
        }
    }
}
