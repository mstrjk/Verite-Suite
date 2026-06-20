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

package teacommontea.veritechasse.check.impl.badpackets;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

@CheckInfo(name = "BadPacketsPitch", description = "Pitch outside the valid [-90, 90] range.")
public final class BadPacketInvalidPitch extends Check implements PacketCheck {

    public BadPacketInvalidPitch(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;
        VeritePacketEvent flying = event;
        if (!flying.hasRotationChanged()) return;

        float pitch = flying.getLocation().getPitch();
        if (pitch > 90.0f || pitch < -90.0f) {
            flag("pitch=" + pitch);
        }
    }
}
