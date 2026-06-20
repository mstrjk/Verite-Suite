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

package teacommontea.veritechasse.check.impl;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.Material;

@CheckInfo(name = "JesusA", description = "Standing on the surface of water.", decay = 0.02)
public final class JesusA extends Check implements PacketCheck, ConfidenceCheck {

    private double innocence = 1.0;

    public JesusA(VeritePlayer player) {
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

        if (JesusExempt.exempt(s)) {
            innocence = 1.0;
            return;
        }

        boolean overWater = s.belowMaterial == Material.WATER || s.feetMaterial == Material.WATER;
        boolean notSubmerged = !s.inWater && !s.swimming;
        boolean noSolidSupport = !s.belowMaterial.isSolid()
                && s.belowMaterial != Material.LILY_PAD;

        if (overWater && notSubmerged && noSolidSupport) {
            setInfo("below=" + s.belowMaterial + " feet=" + s.feetMaterial + " inWater=" + s.inWater);
            innocence = Math.max(0.0, innocence - 0.4);
        } else {
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
