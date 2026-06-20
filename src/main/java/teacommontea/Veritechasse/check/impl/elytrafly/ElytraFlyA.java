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

package teacommontea.veritechasse.check.impl.elytrafly;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "ElytraFlyA", description = "Gliding with non-vanilla hover or sustained lift.", decay = 0.02)
public final class ElytraFlyA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double HOVER_BAND = 0.02;
    private static final int HOVER_REQUIRED = 20;
    private static final int IMPULSE_TICKS = 10;

    private double lastY;
    private boolean hasLast;
    private int hoverTicks;
    private int velocityTimer;
    private double innocence = 1.0;

    public ElytraFlyA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markVelocity() {
        velocityTimer = IMPULSE_TICKS;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (velocityTimer > 0) velocityTimer--;

        if (!s.gliding || s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.riptiding || s.levitation || velocityTimer > 0) {
            hoverTicks = 0;
            innocence = Math.min(1.0, innocence + 0.2);
            return;
        }

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

        boolean hovering = Math.abs(deltaY) < HOVER_BAND;
        boolean lifting = deltaY > HOVER_BAND;

        if (hovering || lifting) {
            hoverTicks++;
            if (hoverTicks > HOVER_REQUIRED) {
                setInfo("deltaY=" + formatOffset(deltaY) + " hoverBand=" + formatOffset(HOVER_BAND) + " hoverTicks=" + hoverTicks);
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            hoverTicks = Math.max(0, hoverTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
