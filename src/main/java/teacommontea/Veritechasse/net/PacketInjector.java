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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public final class PacketInjector {

    private static final String HANDLER = "verite_packet_handler";

    private final Consumer<VeritePacketEvent> inbound;
    private final Consumer<VeritePacketEvent> outbound;

    public PacketInjector(Consumer<VeritePacketEvent> inbound, Consumer<VeritePacketEvent> outbound) {
        this.inbound = inbound;
        this.outbound = outbound;
    }

    public NetUser inject(Player player) {
        try {
            ServerPlayer handle = ((CraftPlayer) player).getHandle();
            Channel channel = handle.connection.connection.channel;
            UUID uuid = player.getUniqueId();
            int entityId = handle.getId();
            NetUser user = new NetUser(uuid, entityId, channel, handle);

            channel.eventLoop().submit(() -> {
                if (channel.pipeline().get(HANDLER) != null) {
                    channel.pipeline().remove(HANDLER);
                }

                channel.pipeline().addBefore("packet_handler", HANDLER, new Handler(user));
            });
            return user;
        } catch (Throwable t) {
            return null;
        }
    }

    public void eject(NetUser user) {
        if (user == null || user.getChannel() == null) return;
        Channel channel = user.getChannel();
        channel.eventLoop().submit(() -> {
            if (channel.pipeline().get(HANDLER) != null) {
                channel.pipeline().remove(HANDLER);
            }
        });
    }

    private final class Handler extends ChannelDuplexHandler {
        private final NetUser user;

        Handler(NetUser user) {
            this.user = user;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Packet<?> packet) {
                VeritePacketType type = classifyInbound(packet);
                if (type != VeritePacketType.OTHER) {
                    VeritePacketEvent event = new VeritePacketEvent(type, user, packet);
                    try {
                        inbound.accept(event);
                    } catch (Throwable ignored) {
                    }
                    if (event.isCancelled()) {
                        return;
                    }
                }
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Packet<?> packet) {
                VeritePacketType type = classifyOutbound(packet);
                if (type != VeritePacketType.OTHER) {
                    VeritePacketEvent event = new VeritePacketEvent(type, user, packet);
                    try {
                        outbound.accept(event);
                    } catch (Throwable ignored) {
                    }
                }
            }
            super.write(ctx, msg, promise);
        }
    }

    private static VeritePacketType classifyInbound(Packet<?> p) {
        if (p instanceof ServerboundMovePlayerPacket) return VeritePacketType.FLYING;
        if (p instanceof ServerboundAttackPacket) return VeritePacketType.ATTACK;
        if (p instanceof ServerboundInteractPacket) return VeritePacketType.INTERACT_ENTITY;
        if (p instanceof ServerboundSwingPacket) return VeritePacketType.ANIMATION;
        if (p instanceof ServerboundPlayerActionPacket) return VeritePacketType.PLAYER_DIGGING;
        if (p instanceof ServerboundUseItemPacket) return VeritePacketType.USE_ITEM;
        if (p instanceof ServerboundUseItemOnPacket) return VeritePacketType.BLOCK_PLACE;
        if (p instanceof ServerboundMoveVehiclePacket) return VeritePacketType.VEHICLE_MOVE;
        if (p instanceof ServerboundSetCarriedItemPacket) return VeritePacketType.HELD_ITEM_CHANGE;
        if (p instanceof ServerboundPlayerInputPacket) return VeritePacketType.PLAYER_INPUT;
        if (p instanceof ServerboundCustomPayloadPacket) return VeritePacketType.PLUGIN_MESSAGE;
        if (p instanceof ServerboundPongPacket) return VeritePacketType.PONG;
        if (p instanceof ServerboundKeepAlivePacket) return VeritePacketType.KEEP_ALIVE;
        return VeritePacketType.OTHER;
    }

    private static VeritePacketType classifyOutbound(Packet<?> p) {
        if (p instanceof ClientboundSetEntityMotionPacket) return VeritePacketType.ENTITY_VELOCITY;
        if (p instanceof ClientboundMoveEntityPacket.PosRot) return VeritePacketType.ENTITY_RELATIVE_MOVE_AND_ROTATION;
        if (p instanceof ClientboundMoveEntityPacket.Pos) return VeritePacketType.ENTITY_RELATIVE_MOVE;
        if (p instanceof ClientboundTeleportEntityPacket) return VeritePacketType.ENTITY_TELEPORT;
        if (p instanceof ClientboundRemoveEntitiesPacket) return VeritePacketType.DESTROY_ENTITIES;
        if (p instanceof ClientboundAddEntityPacket) return VeritePacketType.SPAWN_ENTITY;
        return VeritePacketType.OTHER;
    }
}
