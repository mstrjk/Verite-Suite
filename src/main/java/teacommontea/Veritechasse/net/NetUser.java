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

package teacommontea.veritechasse.net;

import io.netty.channel.Channel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class NetUser {

    private final UUID uuid;
    private final int entityId;
    private final Channel channel;
    private final ServerPlayer handle;

    public NetUser(UUID uuid, int entityId, Channel channel, ServerPlayer handle) {
        this.uuid = uuid;
        this.entityId = entityId;
        this.channel = channel;
        this.handle = handle;
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getEntityId() {
        return entityId;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isPlayState() {
        return channel != null && channel.isActive() && handle.connection != null;
    }

    public void writePacket(Packet<?> packet) {
        try {
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(packet, channel.voidPromise());
            }
        } catch (Throwable ignored) {
        }
    }
}
