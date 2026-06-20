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

package teacommontea.veritechasse.player;

import teacommontea.veritechasse.net.BlockFace;
import teacommontea.veritechasse.net.Vec3f;
import teacommontea.veritechasse.net.Vec3i;
import org.bukkit.Material;

public final class BlockPlace {

    private final VeritePlayer player;
    public final Vec3i position;
    public final BlockFace face;
    public final Material bukkitMaterial;
    public final Material replacedMaterial;
    public final Vec3f cursor;

    public BlockPlace(VeritePlayer player, Vec3i position, BlockFace face,
                      Material bukkitMaterial, Material replacedMaterial, Vec3f cursor) {
        this.player = player;
        this.position = position;
        this.face = face;
        this.bukkitMaterial = bukkitMaterial;
        this.replacedMaterial = replacedMaterial;
        this.cursor = cursor;
    }

    public BlockFace getFace() {
        return face;
    }

    public Vec3f getClickedLocation() {
        return cursor;
    }

    public Material getMaterial() {
        return bukkitMaterial;
    }

    public VeritePlayer getPlayer() {
        return player;
    }
}
