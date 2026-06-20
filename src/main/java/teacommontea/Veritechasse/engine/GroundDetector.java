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

package teacommontea.veritechasse.engine;

import teacommontea.veritechasse.engine.latency.CompensatedWorld;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;

public final class GroundDetector {

    private static final double WIDTH = 0.3;
    private static final double PROBE = 0.001;

    private GroundDetector() {
    }

    public static boolean isOnGround(WorldSnapshot snapshot, double cx, double cz, double y) {
        if (snapshot == null || !snapshot.valid) return false;

        double minX = cx - WIDTH;
        double maxX = cx + WIDTH;
        double minZ = cz - WIDTH;
        double maxZ = cz + WIDTH;

        BoundingBox feet = new BoundingBox(minX, y - PROBE, minZ, maxX, y, maxZ);

        int bx0 = floor(minX), bx1 = floor(maxX);
        int bz0 = floor(minZ), bz1 = floor(maxZ);
        int by0 = floor(y - PROBE - 1.0), by1 = floor(y);

        for (int bx = bx0; bx <= bx1; bx++) {
            for (int bz = bz0; bz <= bz1; bz++) {
                for (int by = by0; by <= by1; by++) {
                    BoundingBox[] boxes = snapshot.collisionAt(bx, by, bz);
                    if (boxes == null || boxes.length == 0) continue;
                    for (BoundingBox box : boxes) {
                        BoundingBox shifted = box.clone().shift(bx, by, bz);
                        if (shifted.overlaps(feet)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isOnGround(VeritePlayer verite, Player player, double y) {
        return isOnGround(player, y, verite.getCompensatedWorld());
    }

    public static boolean isOnGround(Player player, double y) {
        return isOnGround(player, y, null);
    }

    private static boolean isOnGround(Player player, double y, CompensatedWorld compensated) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return false;

        double minX = loc.getX() - WIDTH;
        double maxX = loc.getX() + WIDTH;
        double minZ = loc.getZ() - WIDTH;
        double maxZ = loc.getZ() + WIDTH;

        BoundingBox feet = new BoundingBox(minX, y - PROBE, minZ, maxX, y, maxZ);

        int bx0 = floor(minX), bx1 = floor(maxX);
        int bz0 = floor(minZ), bz1 = floor(maxZ);
        int by0 = floor(y - PROBE - 1.0), by1 = floor(y);

        for (int bx = bx0; bx <= bx1; bx++) {
            for (int bz = bz0; bz <= bz1; bz++) {
                for (int by = by0; by <= by1; by++) {
                    if (compensated != null && compensated.hasCompensatedChange(bx, by, bz)
                            && compensated.getBlock(bx, by, bz) == null) {
                        continue;
                    }
                    Block block = world.getBlockAt(bx, by, bz);
                    if (block.isPassable()) continue;
                    VoxelShape shape = block.getCollisionShape();
                    for (BoundingBox box : shape.getBoundingBoxes()) {
                        BoundingBox world1 = box.shift(bx, by, bz);
                        if (world1.overlaps(feet)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
