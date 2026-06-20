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

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public final class VeritePacketEvent {

    private final VeritePacketType type;
    private final NetUser user;
    private final Packet<?> raw;
    private boolean cancelled;

    public VeritePacketEvent(VeritePacketType type, NetUser user, Packet<?> raw) {
        this.type = type;
        this.user = user;
        this.raw = raw;
    }

    public VeritePacketType getPacketType() {
        return type;
    }

    public NetUser getUser() {
        return user;
    }

    public Packet<?> raw() {
        return raw;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isFlying() {
        return type == VeritePacketType.FLYING;
    }

    public boolean hasPositionChanged() {
        return raw instanceof ServerboundMovePlayerPacket m && m.hasPos;
    }

    public boolean hasRotationChanged() {
        return raw instanceof ServerboundMovePlayerPacket m && m.hasRot;
    }

    public boolean isOnGround() {
        return raw instanceof ServerboundMovePlayerPacket m && m.isOnGround();
    }

    public Loc getLocation() {
        if (raw instanceof ServerboundMovePlayerPacket m) {
            return new Loc(m.x, m.y, m.z, m.yRot, m.xRot);
        }
        return new Loc(0, 0, 0, 0, 0);
    }

    public int getInteractEntityId() {
        if (raw instanceof net.minecraft.network.protocol.game.ServerboundAttackPacket a) return a.entityId();
        if (raw instanceof ServerboundInteractPacket i) return i.entityId();
        return -1;
    }

    public boolean isAttack() {
        return type == VeritePacketType.ATTACK;
    }

    public DiggingAction getDiggingAction() {
        if (raw instanceof ServerboundPlayerActionPacket a) {
            return switch (a.getAction()) {
                case START_DESTROY_BLOCK -> DiggingAction.START_DIGGING;
                case ABORT_DESTROY_BLOCK -> DiggingAction.CANCELLED_DIGGING;
                case STOP_DESTROY_BLOCK -> DiggingAction.FINISHED_DIGGING;
                case RELEASE_USE_ITEM -> DiggingAction.RELEASE_USE_ITEM;
                case DROP_ITEM -> DiggingAction.DROP_ITEM;
                case DROP_ALL_ITEMS -> DiggingAction.DROP_ITEM_STACK;
                case SWAP_ITEM_WITH_OFFHAND -> DiggingAction.SWAP_ITEM_WITH_OFFHAND;
                default -> DiggingAction.OTHER;
            };
        }
        return DiggingAction.OTHER;
    }

    public Vec3i getDigBlockPosition() {
        if (raw instanceof ServerboundPlayerActionPacket a && a.getPos() != null) {
            var p = a.getPos();
            return new Vec3i(p.getX(), p.getY(), p.getZ());
        }
        return new Vec3i(0, 0, 0);
    }

    public int getSlot() {
        if (raw instanceof ServerboundSetCarriedItemPacket h) return h.getSlot();
        return 0;
    }

    public boolean isForward() {
        return raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerInputPacket p && p.input().forward();
    }

    public boolean isBackward() {
        return raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerInputPacket p && p.input().backward();
    }

    public boolean isLeft() {
        return raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerInputPacket p && p.input().left();
    }

    public boolean isRight() {
        return raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerInputPacket p && p.input().right();
    }

    public boolean isJumpInput() {
        return raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerInputPacket p && p.input().jump();
    }

    public boolean isSneakInput() {
        return raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerInputPacket p && p.input().shift();
    }

    public boolean isSprintInput() {
        return raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerInputPacket p && p.input().sprint();
    }

    public boolean isInputPacket() {
        return raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
    }

    public String getChannelName() {
        if (raw instanceof net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket c) {
            return c.payload().type().id().toString();
        }
        return "";
    }

    public byte[] getData() {
        if (raw instanceof net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket c) {
            var payload = c.payload();
            if (payload instanceof net.minecraft.network.protocol.common.custom.BrandPayload b) {
                return b.brand().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            if (payload instanceof net.minecraft.network.protocol.common.custom.DiscardedPayload d) {
                return d.data();
            }
        }
        return new byte[0];
    }

    public enum DiggingAction {
        START_DIGGING, CANCELLED_DIGGING, FINISHED_DIGGING, RELEASE_USE_ITEM,
        DROP_ITEM, DROP_ITEM_STACK, SWAP_ITEM_WITH_OFFHAND, OTHER
    }
}
