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

package teacommontea.veritechasse.engine.entity;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityTracker {

    private static final double DELTA_UNIT = 4096.0;

    private final Map<Integer, TrackedEntity> entities = new ConcurrentHashMap<>();

    public TrackedEntity get(int id) {
        return entities.get(id);
    }

    public void onPacketSend(VeritePacketEvent event) {
        VeritePacketType type = event.getPacketType();

        if (type == VeritePacketType.SPAWN_ENTITY) {
            ClientboundAddEntityPacket w = (ClientboundAddEntityPacket) event.raw();
            boolean player = w.getType() == net.minecraft.world.entity.EntityType.PLAYER;
            spawn(w.getId(), w.getX(), w.getY(), w.getZ(), player);
            return;
        }
        if (type == VeritePacketType.ENTITY_RELATIVE_MOVE
                || type == VeritePacketType.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            ClientboundMoveEntityPacket w = (ClientboundMoveEntityPacket) event.raw();

            int id = moveEntityId(w);
            TrackedEntity e = entities.get(id);
            if (e != null) {
                e.move(w.getXa() / DELTA_UNIT, w.getYa() / DELTA_UNIT, w.getZa() / DELTA_UNIT);
            }
            return;
        }
        if (type == VeritePacketType.ENTITY_TELEPORT) {
            ClientboundTeleportEntityPacket w = (ClientboundTeleportEntityPacket) event.raw();
            TrackedEntity e = entities.get(w.id());
            if (e != null) {
                var c = w.change();
                e.setPosition(c.position().x, c.position().y, c.position().z);
            }
            return;
        }
        if (type == VeritePacketType.DESTROY_ENTITIES) {
            ClientboundRemoveEntitiesPacket w = (ClientboundRemoveEntitiesPacket) event.raw();

            for (Object id : w.getEntityIds()) {
                entities.remove(((Number) id).intValue());
            }
        }
    }

    private static java.lang.reflect.Field moveIdField;
    private static int moveEntityId(ClientboundMoveEntityPacket w) {
        try {
            if (moveIdField == null) {
                moveIdField = ClientboundMoveEntityPacket.class.getDeclaredField("entityId");
                moveIdField.setAccessible(true);
            }
            return moveIdField.getInt(w);
        } catch (Throwable t) {
            return -1;
        }
    }

    private void spawn(int id, double x, double y, double z, boolean player) {
        TrackedEntity e = new TrackedEntity(id, x, y, z);
        e.player = player;
        if (player) {
            e.width = 0.6;
            e.height = 1.8;
        }
        entities.put(id, e);
    }

    public void clear() {
        entities.clear();
    }
}
