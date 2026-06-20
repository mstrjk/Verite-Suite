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

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class VerticalPredictor {

    public static final double DEFAULT_GRAVITY = 0.08;
    public static final double DRAG = 0.98;
    public static final double SLOW_FALLING_GRAVITY = 0.01;

    private static final Attribute GRAVITY_ATTRIBUTE = resolveGravityAttribute();

    private VerticalPredictor() {
    }

    public static double predictNextDeltaY(teacommontea.veritechasse.engine.latency.WorldSnapshot s,
                                           double currentDeltaY) {
        double adjustedY = currentDeltaY;
        if (s.levitation) {
            adjustedY += (0.05 * (s.levitationAmplifier + 1) - currentDeltaY) * 0.2;
        } else {
            double gravity = s.gravity;
            if (currentDeltaY <= 0 && s.slowFalling) {
                gravity = Math.min(gravity, SLOW_FALLING_GRAVITY);
            }
            adjustedY -= gravity;
        }
        return adjustedY * DRAG;
    }

    public static double predictNextDeltaY(Player player, double currentDeltaY) {
        double adjustedY = currentDeltaY;

        Integer levitation = effectAmplifier(player, PotionEffectType.LEVITATION);
        if (levitation != null) {
            adjustedY += (0.05 * (levitation + 1) - currentDeltaY) * 0.2;
        } else {
            double gravity = liveGravity(player);
            boolean slowFalling = player.hasPotionEffect(PotionEffectType.SLOW_FALLING);
            if (currentDeltaY <= 0 && slowFalling) {
                gravity = Math.min(gravity, SLOW_FALLING_GRAVITY);
            }
            adjustedY -= gravity;
        }

        return adjustedY * DRAG;
    }

    public static double liveGravity(Player player) {
        if (GRAVITY_ATTRIBUTE == null) return DEFAULT_GRAVITY;
        AttributeInstance inst = player.getAttribute(GRAVITY_ATTRIBUTE);
        return inst == null ? DEFAULT_GRAVITY : inst.getValue();
    }

    private static Attribute resolveGravityAttribute() {
        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.gravity"));
        if (attr == null) {
            attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("gravity"));
        }
        return attr;
    }

    public static boolean hasLevitation(Player player) {
        return effectAmplifier(player, PotionEffectType.LEVITATION) != null;
    }

    private static Integer effectAmplifier(Player player, PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        return effect == null ? null : effect.getAmplifier();
    }
}
