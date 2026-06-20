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

@CheckInfo(name = "CriticalsB", description = "Fake micro-bounce move packets before an attack (packet crit).", decay = 0.02)
public final class CriticalsB extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MICRO_MIN = 0.005;
    private static final double MICRO_MAX = 0.25;
    private static final int BOUNCE_WINDOW = 4;
    private static final int SUSTAIN = 2;

    private double lastY;
    private boolean hasLastY;
    private int sinceBounceUp;
    private boolean sawMicroUp;
    private int fakeCrits;
    private double innocence = 1.0;

    public CriticalsB(VeritePlayer player) {
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
            if (!flying.hasPositionChanged()) return;
            double y = flying.getLocation().getY();
            if (!hasLastY) {
                lastY = y;
                hasLastY = true;
                return;
            }
            double deltaY = y - lastY;
            lastY = y;

            if (sawMicroUp) sinceBounceUp++;
            if (deltaY > MICRO_MIN && deltaY < MICRO_MAX && player.onGround) {
                sawMicroUp = true;
                sinceBounceUp = 0;
            } else if (sinceBounceUp > BOUNCE_WINDOW) {
                sawMicroUp = false;
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

        if (sawMicroUp && sinceBounceUp <= BOUNCE_WINDOW && s.bukkitOnGround) {
            fakeCrits++;
            sawMicroUp = false;
            if (fakeCrits >= SUSTAIN) {
                setInfo("sinceBounceUp=" + String.valueOf(sinceBounceUp) + " bounceWindow=" + String.valueOf(BOUNCE_WINDOW) + " fakeCrits=" + String.valueOf(fakeCrits));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            fakeCrits = Math.max(0, fakeCrits - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
