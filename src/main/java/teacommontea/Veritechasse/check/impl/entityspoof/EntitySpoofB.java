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

package teacommontea.veritechasse.check.impl.entityspoof;

import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.entity.Player;

@CheckInfo(name = "EntitySpoofB", description = "Controlling a mount without the required tooling.", decay = 0.0)
public final class EntitySpoofB extends Check implements ConfidenceCheck {

    private double innocence = 1.0;

    public EntitySpoofB(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void evaluate(Player bukkit) {
        WorldSnapshot.VehicleSnapshot v = player.vehicle;
        if (!v.present || !v.vanillaRideable) {
            innocence = 1.0;
            return;
        }
        if (v.requiresTooling && v.playerIsController && !v.hasRequiredTooling) {
            setInfo("requiresTooling=" + v.requiresTooling + " controller=" + v.playerIsController + " hasTooling=" + v.hasRequiredTooling);
            innocence = 0.0;
        } else {
            innocence = 1.0;
        }
    }
}
