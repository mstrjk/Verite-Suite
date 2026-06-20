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

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class BreakTimeResolver {

    private static Method craftBlockGetState;
    private static Method craftBlockGetPosition;
    private static Method craftBlockGetCraftWorld;
    private static Method craftWorldGetHandle;
    private static Method craftPlayerGetHandle;
    private static Method getDestroyProgress;
    private static boolean resolved;
    private static boolean failed;

    private BreakTimeResolver() {
    }

    public static int minBreakTicks(Player player, Block block) {
        if (failed) return 0;
        if (!resolved) resolve(player, block);
        if (failed) return 0;

        try {
            Object blockState = craftBlockGetState.invoke(block);
            Object blockPos = craftBlockGetPosition.invoke(block);
            Object craftWorld = craftBlockGetCraftWorld.invoke(block);
            Object level = craftWorldGetHandle.invoke(craftWorld);
            Object nmsPlayer = craftPlayerGetHandle.invoke(player);

            float progress = (float) getDestroyProgress.invoke(blockState, nmsPlayer, level, blockPos);
            if (progress <= 0.0f || !Float.isFinite(progress)) return 0;
            if (progress >= 1.0f) return 0;
            return (int) Math.ceil(1.0f / progress);
        } catch (Throwable t) {
            failed = true;
            return 0;
        }
    }

    private static synchronized void resolve(Player player, Block block) {
        if (resolved) return;
        try {
            Class<?> craftBlock = block.getClass();
            craftBlockGetState = craftBlock.getMethod("getBlockState");
            craftBlockGetPosition = craftBlock.getMethod("getPosition");
            craftBlockGetCraftWorld = craftBlock.getMethod("getCraftWorld");

            Object craftWorld = craftBlockGetCraftWorld.invoke(block);
            craftWorldGetHandle = craftWorld.getClass().getMethod("getHandle");

            craftPlayerGetHandle = player.getClass().getMethod("getHandle");

            Object blockState = craftBlockGetState.invoke(block);
            Object blockPos = craftBlockGetPosition.invoke(block);
            Object level = craftWorldGetHandle.invoke(craftWorld);
            Object nmsPlayer = craftPlayerGetHandle.invoke(player);

            getDestroyProgress = findDestroyProgress(blockState, nmsPlayer, level, blockPos);
            if (getDestroyProgress == null) {
                failed = true;
                return;
            }
            resolved = true;
        } catch (Throwable t) {
            failed = true;
        }
    }

    private static Method findDestroyProgress(Object blockState, Object nmsPlayer, Object level, Object blockPos) {
        for (Method m : blockState.getClass().getMethods()) {
            if (m.getReturnType() != float.class) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != 3) continue;
            if (!p[0].isInstance(nmsPlayer)) continue;
            if (!p[1].isInstance(level)) continue;
            if (!p[2].isInstance(blockPos)) continue;
            m.setAccessible(true);
            return m;
        }
        return null;
    }
}
