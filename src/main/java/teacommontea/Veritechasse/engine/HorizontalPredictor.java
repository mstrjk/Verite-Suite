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

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class HorizontalPredictor {

    public static final float AIR_FRICTION = 0.91f;
    public static final float DEFAULT_FRICTION = 0.6f;
    public static final double GROUND_ACCEL_NUMERATOR = 0.21600002;
    public static final float AIR_ACCEL_WALK = 0.02f;
    public static final float AIR_ACCEL_SPRINT = 0.026f;
    public static final double DEFAULT_MOVEMENT_SPEED = 0.1;

    private static final Attribute SPEED_ATTRIBUTE = resolveSpeedAttribute();
    private static final Attribute EFFICIENCY_ATTRIBUTE = resolveEfficiencyAttribute();

    private HorizontalPredictor() {
    }

    public static float blockFriction(Material material) {
        switch (material) {
            case ICE:
            case PACKED_ICE:
            case FROSTED_ICE:
                return 0.98f;
            case BLUE_ICE:
                return 0.989f;
            case SLIME_BLOCK:
            case HONEY_BLOCK:
                return 0.8f;
            default:
                return DEFAULT_FRICTION;
        }
    }

    public static double movementSpeed(Player player) {
        if (SPEED_ATTRIBUTE == null) return DEFAULT_MOVEMENT_SPEED;
        AttributeInstance inst = player.getAttribute(SPEED_ATTRIBUTE);
        return inst == null ? DEFAULT_MOVEMENT_SPEED : inst.getValue();
    }

    public static double groundSpeed(double movementSpeed, float blockFriction) {
        double f = blockFriction;
        return movementSpeed * (GROUND_ACCEL_NUMERATOR / (f * f * f));
    }

    public static double maxNextHorizontal(teacommontea.veritechasse.engine.latency.WorldSnapshot s,
                                           double lastHorizontal, boolean onGround) {
        Material below = s.belowMaterial;
        Material in = s.feetMaterial;
        float friction = onGround ? blockFriction(below) : AIR_FRICTION;
        double speed = s.movementSpeed;

        double accel;
        if (onGround) {
            accel = groundSpeed(speed, blockFriction(below));
        } else {
            accel = s.sprinting ? AIR_ACCEL_SPRINT : AIR_ACCEL_WALK;
        }

        double next = (lastHorizontal * friction) + accel;
        return next * blockSpeedFactor(s, in, below);
    }

    public static float blockSpeedFactor(teacommontea.veritechasse.engine.latency.WorldSnapshot s,
                                         Material in, Material below) {
        float factor = rawSpeedFactor(in);
        if (factor == 1.0f && in != Material.WATER && in != Material.BUBBLE_COLUMN) {
            factor = rawSpeedFactor(below);
        }
        double efficiency = s.movementEfficiency;
        return (float) (factor + efficiency * (1.0f - factor));
    }

    public static double maxNextHorizontal(Player player, double lastHorizontal, boolean onGround) {
        Block below = player.getLocation().getBlock().getRelative(0, -1, 0);
        Block in = player.getLocation().getBlock();
        float friction = onGround ? blockFriction(below.getType()) : AIR_FRICTION;
        double speed = movementSpeed(player);

        double accel;
        if (onGround) {
            accel = groundSpeed(speed, blockFriction(below.getType()));
        } else {
            accel = player.isSprinting() ? AIR_ACCEL_SPRINT : AIR_ACCEL_WALK;
        }

        double next = (lastHorizontal * friction) + accel;
        return next * blockSpeedFactor(player, in.getType(), below.getType());
    }

    public static float blockSpeedFactor(Player player, Material in, Material below) {
        float factor = rawSpeedFactor(in);
        if (factor == 1.0f && in != Material.WATER && in != Material.BUBBLE_COLUMN) {
            factor = rawSpeedFactor(below);
        }
        return modernVelocityMultiplier(player, factor);
    }

    private static float rawSpeedFactor(Material type) {
        if (type == Material.HONEY_BLOCK) return 0.4f;
        if (type == Material.SOUL_SAND) return 0.4f;
        return 1.0f;
    }

    private static float modernVelocityMultiplier(Player player, float blockSpeedFactor) {
        double efficiency = movementEfficiency(player);
        return (float) (blockSpeedFactor + efficiency * (1.0f - blockSpeedFactor));
    }

    public static double movementEfficiency(Player player) {
        if (EFFICIENCY_ATTRIBUTE == null) return 0.0;
        AttributeInstance inst = player.getAttribute(EFFICIENCY_ATTRIBUTE);
        return inst == null ? 0.0 : inst.getValue();
    }

    private static Attribute resolveSpeedAttribute() {
        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.movement_speed"));
        if (attr == null) {
            attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("movement_speed"));
        }
        return attr;
    }

    private static Attribute resolveEfficiencyAttribute() {
        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.movement_efficiency"));
        if (attr == null) {
            attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("movement_efficiency"));
        }
        return attr;
    }
}
