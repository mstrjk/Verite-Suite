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
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.Material;

final class PlacedStateResolver {

    private PlacedStateResolver() {
    }

    enum Kind {
        YAW_ORIENTED,
        FACE_ATTACHED,
        OTHER
    }

    static Kind kindOf(Material type) {
        String n = type.name();
        if (n.contains("LEVER") || n.contains("BUTTON")) {
            return Kind.FACE_ATTACHED;
        }
        if (n.contains("STAIRS") || n.contains("OBSERVER") || n.contains("REPEATER")
                || n.contains("COMPARATOR") || n.contains("TRAPDOOR") || n.endsWith("_DOOR")
                || n.contains("ANVIL")) {
            return Kind.YAW_ORIENTED;
        }
        return Kind.OTHER;
    }

    static BlockFace resolveFacing(VeritePlayer player, BlockPlace place) {
        Material type = place.bukkitMaterial;
        switch (kindOf(type)) {
            case YAW_ORIENTED:
                return PlacementGeometry.placedFacingFromYaw(player.yaw);

            case FACE_ATTACHED: {
                BlockFace clicked = place.getFace();
                if (clicked == BlockFace.UP || clicked == BlockFace.DOWN) {
                    return PlacementGeometry.placedFacingFromYaw(player.yaw);
                }
                return PlacementGeometry.opposite(clicked);
            }

            default:
                return null;
        }
    }

    static boolean resolveTopHalf(BlockPlace place) {
        float cursorY = place.cursor != null ? place.cursor.getY() : 0f;
        return PlacementGeometry.expectTopHalf(place.getFace(), cursorY);
    }
}
