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

package teacommontea.veritechasse.engine.latency;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;

public final class WorldSnapshot {

    public boolean valid;

    public GameMode gameMode = GameMode.SURVIVAL;
    public boolean flying;
    public boolean gliding;
    public boolean riptiding;
    public boolean insideVehicle;
    public boolean sprinting;
    public boolean swimming;
    public boolean inWater;
    public boolean handRaised;
    public int handRaisedTime;
    public boolean bukkitOnGround;

    public boolean dolphinsGrace;
    public boolean levitation;
    public int levitationAmplifier;
    public boolean slowFalling;
    public boolean speedEffect;
    public int speedAmplifier;
    public int jumpBoostAmplifier;
    public boolean frostWalkerBoots;
    public int depthStriderLevel;

    public double movementSpeed = 0.1;
    public double movementEfficiency;
    public double gravity = 0.08;
    public int worldMinHeight = -64;
    public boolean fallDamagePossible = true;

    public Material feetMaterial = Material.AIR;
    public Material belowMaterial = Material.AIR;
    public boolean feetTrapdoorOpen;
    public boolean horizontalCollision;

    private int gridMinX, gridMinY, gridMinZ;
    private int gridSizeX, gridSizeY, gridSizeZ;
    private Material[] gridMaterials;
    private BoundingBox[][] gridCollision;

    private static final int RADIUS_XZ = 2;
    private static final int RADIUS_DOWN = 3;
    private static final int RADIUS_UP = 2;

    public void capture(Player p, double speedAttr, double efficiencyAttr, double gravityAttr) {
        Location loc = p.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            valid = false;
            return;
        }
        this.worldMinHeight = world.getMinHeight();
        Boolean worldFallDamage = world.getGameRuleValue(org.bukkit.GameRules.FALL_DAMAGE);
        this.fallDamagePossible = (worldFallDamage == null || worldFallDamage) && !p.isInvulnerable();

        this.gameMode = p.getGameMode();
        this.flying = p.isFlying();
        this.gliding = p.isGliding();
        this.riptiding = p.isRiptiding();
        this.insideVehicle = p.isInsideVehicle();
        this.sprinting = p.isSprinting();
        this.swimming = p.isSwimming();
        this.inWater = p.isInWater();
        this.handRaised = p.isHandRaised();
        this.handRaisedTime = p.getHandRaisedTime();
        this.bukkitOnGround = teacommontea.veritechasse.engine.GroundDetector.isOnGround(p, loc.getY());

        this.dolphinsGrace = p.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE);
        PotionEffect lev = p.getPotionEffect(PotionEffectType.LEVITATION);
        this.levitation = lev != null;
        this.levitationAmplifier = lev == null ? 0 : lev.getAmplifier();
        this.slowFalling = p.hasPotionEffect(PotionEffectType.SLOW_FALLING);
        PotionEffect spd = p.getPotionEffect(PotionEffectType.SPEED);
        this.speedEffect = spd != null;
        this.speedAmplifier = spd == null ? 0 : spd.getAmplifier();
        PotionEffect jump = p.getPotionEffect(PotionEffectType.JUMP_BOOST);
        this.jumpBoostAmplifier = jump == null ? 0 : jump.getAmplifier() + 1;
        var boots = p.getInventory().getBoots();
        this.frostWalkerBoots = boots != null
                && boots.containsEnchantment(org.bukkit.enchantments.Enchantment.FROST_WALKER);
        this.depthStriderLevel = boots == null ? 0
                : boots.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DEPTH_STRIDER);

        this.movementSpeed = speedAttr;
        this.movementEfficiency = efficiencyAttr;
        this.gravity = gravityAttr;

        Block feet = loc.getBlock();
        Block below = feet.getRelative(0, -1, 0);
        this.feetMaterial = feet.getType();
        this.belowMaterial = below.getType();
        this.feetTrapdoorOpen = feet.getType().name().endsWith("_TRAPDOOR")
                && feet.getBlockData() instanceof Openable o && o.isOpen();

        captureGrid(world, feet.getX(), feet.getY(), feet.getZ());
        this.horizontalCollision = computeHorizontalCollision(loc.getX(), loc.getY(), loc.getZ());
        this.valid = true;
    }

    private boolean computeHorizontalCollision(double px, double py, double pz) {
        double half = 0.3;
        double probe = 0.08;
        BoundingBox body = new BoundingBox(
                px - half - probe, py + 0.1, pz - half - probe,
                px + half + probe, py + 1.7, pz + half + probe);

        int bx0 = floorI(px - half - probe), bx1 = floorI(px + half + probe);
        int by0 = floorI(py + 0.1), by1 = floorI(py + 1.7);
        int bz0 = floorI(pz - half - probe), bz1 = floorI(pz + half + probe);

        for (int bx = bx0; bx <= bx1; bx++) {
            for (int by = by0; by <= by1; by++) {
                for (int bz = bz0; bz <= bz1; bz++) {
                    BoundingBox[] boxes = collisionAt(bx, by, bz);
                    if (boxes == null || boxes.length == 0) continue;
                    for (BoundingBox box : boxes) {
                        BoundingBox shifted = box.clone().shift(bx, by, bz);
                        if (shifted.overlaps(body)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static int floorI(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }

    private void captureGrid(World world, int cx, int cy, int cz) {
        gridMinX = cx - RADIUS_XZ;
        gridMinY = cy - RADIUS_DOWN;
        gridMinZ = cz - RADIUS_XZ;
        gridSizeX = RADIUS_XZ * 2 + 1;
        gridSizeY = RADIUS_DOWN + RADIUS_UP + 1;
        gridSizeZ = RADIUS_XZ * 2 + 1;

        int total = gridSizeX * gridSizeY * gridSizeZ;
        if (gridMaterials == null || gridMaterials.length != total) {
            gridMaterials = new Material[total];
            gridCollision = new BoundingBox[total][];
        }

        int i = 0;
        for (int dx = 0; dx < gridSizeX; dx++) {
            for (int dy = 0; dy < gridSizeY; dy++) {
                for (int dz = 0; dz < gridSizeZ; dz++) {
                    Block b = world.getBlockAt(gridMinX + dx, gridMinY + dy, gridMinZ + dz);
                    gridMaterials[i] = b.getType();
                    if (b.isPassable()) {
                        gridCollision[i] = EMPTY;
                    } else {
                        VoxelShape shape = b.getCollisionShape();
                        gridCollision[i] = shape.getBoundingBoxes().toArray(new BoundingBox[0]);
                    }
                    i++;
                }
            }
        }
    }

    public Material materialAt(int x, int y, int z) {
        int idx = index(x, y, z);
        if (idx < 0) return null;
        return gridMaterials[idx];
    }

    public BoundingBox[] collisionAt(int x, int y, int z) {
        int idx = index(x, y, z);
        if (idx < 0) return null;
        return gridCollision[idx];
    }

    private int index(int x, int y, int z) {
        int dx = x - gridMinX;
        int dy = y - gridMinY;
        int dz = z - gridMinZ;
        if (dx < 0 || dx >= gridSizeX || dy < 0 || dy >= gridSizeY || dz < 0 || dz >= gridSizeZ) {
            return -1;
        }
        return (dx * gridSizeY + dy) * gridSizeZ + dz;
    }

    private static final BoundingBox[] EMPTY = new BoundingBox[0];

    public static final class VehicleSnapshot {
        public boolean present;
        public boolean flyer;
        public boolean boat;
        public GameMode gameMode = GameMode.SURVIVAL;
        public Entity entity;
        public Material atMaterial = Material.AIR;
        public Material belowMaterial = Material.AIR;
        public boolean solidUnder;
        public boolean inWater;

        public boolean vanillaRideable;
        public boolean controllable;
        public boolean requiresTooling;
        public boolean hasRequiredTooling;
        public boolean wild;
        public boolean playerIsController;
        public boolean living;
        public boolean horse;
        public double mountSpeedAttr;
        public double horseJumpStrength;
    }
}
