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

import teacommontea.veritechasse.net.BlockFace;

final class PlacementGeometry {

    private PlacementGeometry() {
    }

    static BlockFace fromYaw(float yaw) {
        int data = (int) Math.floor(yaw / 90.0 + 0.5) & 3;
        switch (data) {
            case 0: return BlockFace.SOUTH;
            case 1: return BlockFace.WEST;
            case 2: return BlockFace.NORTH;
            default: return BlockFace.EAST;
        }
    }

    static BlockFace placedFacingFromYaw(float yaw) {
        return opposite(fromYaw(yaw));
    }

    static boolean expectTopHalf(BlockFace clickedFace, float cursorY) {
        if (clickedFace == BlockFace.DOWN) return true;
        return clickedFace != BlockFace.UP && cursorY > 0.5f;
    }

    static BlockFace opposite(BlockFace f) {
        switch (f) {
            case NORTH: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.NORTH;
            case WEST: return BlockFace.EAST;
            case EAST: return BlockFace.WEST;
            case UP: return BlockFace.DOWN;
            case DOWN: return BlockFace.UP;
            default: return f;
        }
    }
}
