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

package teacommontea.veritechasse.check.impl.entityspoof;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Steerable;
import org.bukkit.entity.Tameable;

public final class MountData {

    private MountData() {
    }

    private static final String HAPPY_GHAST = "HAPPY_GHAST";

    public static boolean isVanillaRideable(EntityType type) {
        if (type.name().equals(HAPPY_GHAST)) return true;
        switch (type) {
            case PIG:
            case STRIDER:
            case HORSE:
            case DONKEY:
            case MULE:
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
            case CAMEL:
            case LLAMA:
            case TRADER_LLAMA:
                return true;
            default:
                return false;
        }
    }

    public static boolean isFlyer(EntityType type) {
        return type.name().equals(HAPPY_GHAST);
    }

    public static boolean isControllable(EntityType type) {
        if (type.name().equals(HAPPY_GHAST)) return true;
        switch (type) {
            case PIG:
            case STRIDER:
            case HORSE:
            case DONKEY:
            case MULE:
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
            case CAMEL:
                return true;
            default:
                return false;
        }
    }

    public static boolean hasRequiredTooling(Entity mount) {
        if (mount instanceof Steerable s) {
            return s.hasSaddle();
        }
        if (mount instanceof AbstractHorse h) {
            return h.getInventory().getSaddle() != null;
        }
        return true;
    }

    public static boolean requiresTooling(EntityType type) {
        switch (type) {
            case PIG:
            case STRIDER:
            case HORSE:
            case DONKEY:
            case MULE:
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
            case CAMEL:
                return true;
            default:
                return false;
        }
    }

    public static boolean isWild(Entity mount) {
        if (mount instanceof AbstractHorse h) {
            return !h.isTamed();
        }
        if (mount instanceof Tameable t) {
            return !t.isTamed();
        }
        return false;
    }
}
