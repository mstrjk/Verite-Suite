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

package teacommontea.veritechasse.listener;

import teacommontea.veritechasse.AntiCheat;
import teacommontea.veritechasse.net.NetUser;
import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.net.Vec3d;
import teacommontea.veritechasse.player.VeritePlayer;

public final class PacketListener {

    private final AntiCheat antiCheat;

    public PacketListener(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }

    public void onPacketReceive(VeritePacketEvent event) {
        NetUser user = event.getUser();
        if (user == null || user.getUUID() == null) return;

        VeritePlayer player = antiCheat.getPlayerDataManager().get(user.getUUID());
        if (player == null) return;

        if (event.getPacketType() == VeritePacketType.PONG) {

            int id = ((net.minecraft.network.protocol.common.ServerboundPongPacket) event.raw()).getId();
            player.getTransactions().handleResponse(id);
            return;
        }

        player.getChecks().onPacketReceive(event);

        if (event.getPacketType() == VeritePacketType.VEHICLE_MOVE) {
            var v = ((net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket) event.raw()).position();
            player.getChecks().onVehicleMove(v.x, v.y, v.z);
        }

        if (event.isFlying()) {
            player.getTransactions().sendTransaction();
        }
    }

    public void onPacketSend(VeritePacketEvent event) {
        NetUser user = event.getUser();
        if (user == null || user.getUUID() == null) return;
        VeritePlayer player = antiCheat.getPlayerDataManager().get(user.getUUID());
        if (player == null) return;
        player.entityTracker.onPacketSend(event);

        if (event.getPacketType() == VeritePacketType.ENTITY_VELOCITY) {
            var w = (net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket) event.raw();
            if (w.id() == user.getEntityId()) {
                var v = w.movement();
                double mag = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
                player.getChecks().exempt(velocityExemptTicks(mag));
            }
        }
    }

    private static int velocityExemptTicks(double magnitude) {
        int ticks = 15 + (int) (magnitude * 12.0);
        return Math.min(60, Math.max(15, ticks));
    }
}
