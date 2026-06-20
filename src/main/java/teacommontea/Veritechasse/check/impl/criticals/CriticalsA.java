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

package teacommontea.veritechasse.check.impl.criticals;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "CriticalsA", description = "Claiming a critical hit while really on the ground.", decay = 0.02)
public final class CriticalsA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MICRO = 0.02;
    private static final int SUSTAIN = 2;

    private double lastY;
    private boolean hasLastY;
    private boolean lastClaimedAir;
    private double lastDeltaY;
    private int fakeHits;
    private double innocence = 1.0;

    public CriticalsA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.isFlying()) {
            VeritePacketEvent flying = event;
            lastClaimedAir = !flying.isOnGround();
            if (flying.hasPositionChanged()) {
                double y = flying.getLocation().getY();
                if (hasLastY) lastDeltaY = y - lastY;
                lastY = y;
                hasLastY = true;
            } else {
                lastDeltaY = 0.0;
            }
            return;
        }

        if (event.getPacketType() != VeritePacketType.ATTACK) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle || s.levitation) {
            return;
        }

        boolean reallyGrounded = s.bukkitOnGround;
        boolean barelyMoving = Math.abs(lastDeltaY) < MICRO;

        if (lastClaimedAir && reallyGrounded && barelyMoving) {
            fakeHits++;
            if (fakeHits >= SUSTAIN) {
                setInfo("deltaY=" + formatOffset(lastDeltaY) + " microMax=" + formatOffset(MICRO) + " claimedAir=" + String.valueOf(lastClaimedAir));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            fakeHits = Math.max(0, fakeHits - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
