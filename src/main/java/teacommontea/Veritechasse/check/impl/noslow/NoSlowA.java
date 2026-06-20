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
import org.bukkit.Material;

@CheckInfo(name = "NoSlowA", description = "Moving too fast through a slowing block.", decay = 0.0)
public final class NoSlowA extends Check implements PacketCheck, ConfidenceCheck {

    private static final long TIGHT_MISTIME_MS = 150L;
    private static final int SUSTAIN = 2;

    private double lastX, lastZ;
    private boolean hasLast;
    private int overTicks;
    private double innocence = 1.0;

    public NoSlowA(VeritePlayer player) {
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

        double slowFactor = slowFactor(s.feetMaterial, s.belowMaterial);
        if (slowFactor >= 1.0) {
            innocence = Math.min(1.0, innocence + 0.3);
            overTicks = 0;
            return;
        }

        if (System.currentTimeMillis() - player.lastExternalEvent < TIGHT_MISTIME_MS) {
            innocence = Math.min(1.0, innocence + 0.1);
            return;
        }

        double maxAllowed = baseSpeedFor(s) * slowFactor;
        if (horizontal > maxAllowed) {
            overTicks++;
            if (overTicks >= SUSTAIN) {
                setInfo("horizontal=" + formatOffset(horizontal) + " maxAllowed=" + formatOffset(maxAllowed) + " slowFactor=" + formatOffset(slowFactor));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            overTicks = 0;
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }

    private double baseSpeedFor(WorldSnapshot s) {
        double base = 0.215;
        if (s.sprinting) base = 0.281;
        return base;
    }

    private static double slowFactor(Material in, Material below) {
        if (in == Material.COBWEB) return 0.10;
        if (in == Material.SWEET_BERRY_BUSH) return 0.20;
        if (in == Material.POWDER_SNOW) return 0.30;
        if (below == Material.SOUL_SAND) return 0.40;
        if (below == Material.HONEY_BLOCK || in == Material.HONEY_BLOCK) return 0.40;
        return 1.0;
    }
}
