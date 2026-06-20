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

public enum VeritePacketType {

    FLYING,
    ATTACK,
    INTERACT_ENTITY,
    ANIMATION,
    PLAYER_DIGGING,
    USE_ITEM,
    BLOCK_PLACE,
    VEHICLE_MOVE,
    HELD_ITEM_CHANGE,
    PLAYER_INPUT,
    PLUGIN_MESSAGE,
    PONG,
    KEEP_ALIVE,

    ENTITY_VELOCITY,
    ENTITY_RELATIVE_MOVE,
    ENTITY_RELATIVE_MOVE_AND_ROTATION,
    ENTITY_TELEPORT,
    DESTROY_ENTITIES,
    SPAWN_ENTITY,

    OTHER
}
