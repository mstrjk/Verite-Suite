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

import org.bukkit.Material;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CompensatedWorld {

    private final TransactionTracker tracker;

    private final Map<Long, Material> changes = new ConcurrentHashMap<>();
    private final java.util.Set<Long> keys = ConcurrentHashMap.newKeySet();

    public CompensatedWorld(TransactionTracker tracker) {
        this.tracker = tracker;
    }

    public void updateBlock(int x, int y, int z, Material state) {
        long key = key(x, y, z);
        tracker.getTaskQueue().addRealTimeTask(tracker.lastTransactionSent(), () -> {
            if (state == null || state == Material.AIR) {
                changes.remove(key);
                keys.add(key);
            } else {
                changes.put(key, state);
                keys.add(key);
            }
        });
    }

    public Material getBlock(int x, int y, int z) {
        return changes.get(key(x, y, z));
    }

    public boolean hasCompensatedChange(int x, int y, int z) {
        return keys.contains(key(x, y, z));
    }

    private static long key(int x, int y, int z) {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) z & 0x3FFFFFF) << 12) | ((long) y & 0xFFF);
    }
}
