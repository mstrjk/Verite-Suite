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

package teacommontea.veritechasse;

final class SnapshotCapture {

    private static final org.bukkit.attribute.Attribute SPEED_ATTRIBUTE = resolveSpeedAttribute();

    private static org.bukkit.attribute.Attribute resolveSpeedAttribute() {
        try {
            return org.bukkit.Registry.ATTRIBUTE.get(
                    org.bukkit.NamespacedKey.minecraft("generic.movement_speed"));
        } catch (Throwable t) {
            return null;
        }
    }

    void captureSnapshot(teacommontea.veritechasse.player.VeritePlayer data, org.bukkit.entity.Player p) {
        double speed = teacommontea.veritechasse.engine.HorizontalPredictor.movementSpeed(p);
        double efficiency = teacommontea.veritechasse.engine.HorizontalPredictor.movementEfficiency(p);
        double gravity = teacommontea.veritechasse.engine.VerticalPredictor.liveGravity(p);
        data.snapshot.capture(p, speed, efficiency, gravity);
        captureVehicle(data, p);
    }

    private void captureVehicle(teacommontea.veritechasse.player.VeritePlayer data, org.bukkit.entity.Player p) {
        teacommontea.veritechasse.engine.latency.WorldSnapshot.VehicleSnapshot v = data.vehicle;
        org.bukkit.entity.Entity mount = p.getVehicle();
        if (mount == null) {
            v.present = false;
            v.entity = null;
            return;
        }
        v.present = true;
        v.entity = mount;
        v.gameMode = p.getGameMode();
        v.boat = mount instanceof org.bukkit.entity.Boat;
        v.flyer = teacommontea.veritechasse.check.impl.entityspoof.MountData.isFlyer(mount.getType());
        org.bukkit.Location ml = mount.getLocation();
        org.bukkit.World world = ml.getWorld();
        if (world == null) {
            v.present = false;
            return;
        }
        int bx = ml.getBlockX();
        int by = ml.getBlockY();
        int bz = ml.getBlockZ();
        v.atMaterial = world.getBlockAt(bx, by, bz).getType();
        v.belowMaterial = world.getBlockAt(bx, by - 1, bz).getType();
        v.solidUnder = v.belowMaterial.isSolid();
        v.inWater = v.atMaterial == org.bukkit.Material.WATER || v.belowMaterial == org.bukkit.Material.WATER
                || v.atMaterial == org.bukkit.Material.BUBBLE_COLUMN;

        org.bukkit.entity.EntityType type = mount.getType();
        v.vanillaRideable = teacommontea.veritechasse.check.impl.entityspoof.MountData.isVanillaRideable(type);
        v.controllable = teacommontea.veritechasse.check.impl.entityspoof.MountData.isControllable(type);
        v.requiresTooling = teacommontea.veritechasse.check.impl.entityspoof.MountData.requiresTooling(type);
        v.hasRequiredTooling = teacommontea.veritechasse.check.impl.entityspoof.MountData.hasRequiredTooling(mount);
        v.wild = teacommontea.veritechasse.check.impl.entityspoof.MountData.isWild(mount);
        v.living = mount instanceof org.bukkit.entity.LivingEntity;
        v.horse = mount instanceof org.bukkit.entity.AbstractHorse;

        java.util.List<org.bukkit.entity.Entity> passengers = mount.getPassengers();
        v.playerIsController = !passengers.isEmpty() && passengers.get(0).equals(p);

        v.mountSpeedAttr = 0.25;
        if (mount instanceof org.bukkit.entity.LivingEntity living && SPEED_ATTRIBUTE != null) {
            org.bukkit.attribute.AttributeInstance inst = living.getAttribute(SPEED_ATTRIBUTE);
            if (inst != null) v.mountSpeedAttr = inst.getValue();
        }
        v.horseJumpStrength = mount instanceof org.bukkit.entity.AbstractHorse h ? h.getJumpStrength() : 0.0;
    }

    boolean computeGuiExempt(org.bukkit.entity.Player p) {
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE
                || p.getGameMode() == org.bukkit.GameMode.SPECTATOR
                || p.isFlying() || p.isGliding() || p.isInsideVehicle()) {
            return true;
        }
        org.bukkit.block.Block at = p.getLocation().getBlock();
        org.bukkit.block.Block below = at.getRelative(0, -1, 0);
        org.bukkit.Material atM = at.getType();
        org.bukkit.Material belowM = below.getType();
        if (atM == org.bukkit.Material.WATER || atM == org.bukkit.Material.LAVA
                || belowM == org.bukkit.Material.WATER || atM == org.bukkit.Material.BUBBLE_COLUMN
                || belowM == org.bukkit.Material.ICE || belowM == org.bukkit.Material.PACKED_ICE
                || belowM == org.bukkit.Material.BLUE_ICE || belowM == org.bukkit.Material.FROSTED_ICE) {
            return true;
        }
        for (org.bukkit.entity.Entity e : p.getNearbyEntities(0.8, 0.8, 0.8)) {
            if (e instanceof org.bukkit.entity.Player
                    || e.getType().name().contains("BOAT")
                    || e.getType().name().contains("MINECART")) {
                return true;
            }
        }
        return false;
    }
}
