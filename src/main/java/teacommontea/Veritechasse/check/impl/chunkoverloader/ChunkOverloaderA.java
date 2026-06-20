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

package teacommontea.veritechasse.check.impl.chunkoverloader;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@CheckInfo(name = "ChunkOverloaderA", description = "Crossing chunk boundaries at an unrealistic rate.", decay = 0.02)
public final class ChunkOverloaderA extends Check implements PacketCheck, ConfidenceCheck {

    private static final long WINDOW_MS = 1000L;
    private static final int MAX_CHUNK_CHANGES = 4;
    private static final int SUSTAIN = 3;

    private long lastChunk = Long.MIN_VALUE;
    private final Deque<long[]> recent = new ArrayDeque<>();
    private final Set<Long> recentSet = new HashSet<>();
    private int overTicks;
    private int teleportTimer;
    private double innocence = 1.0;

    public ChunkOverloaderA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markTeleport() {
        teleportTimer = 20;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (teleportTimer > 0) {
            teleportTimer--;
        }

        VeritePacketEvent flying = event;
        if (!flying.hasPositionChanged()) return;

        if (s.gameMode == GameMode.SPECTATOR) return;

        long now = System.currentTimeMillis();

        while (!recent.isEmpty() && now - recent.peekFirst()[1] > WINDOW_MS) {
            long[] old = recent.pollFirst();
            recentSet.remove(old[0]);
        }

        int chunkX = (int) Math.floor(flying.getLocation().getX()) >> 4;
        int chunkZ = (int) Math.floor(flying.getLocation().getZ()) >> 4;
        long chunk = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);

        boolean newChunk = chunk != lastChunk;
        lastChunk = chunk;

        if (newChunk && teleportTimer <= 0 && recentSet.add(chunk)) {
            recent.addLast(new long[]{chunk, now});
        }

        int distinct = recentSet.size();
        if (distinct > MAX_CHUNK_CHANGES) {
            overTicks++;
            if (overTicks >= SUSTAIN) {
                int over = distinct - MAX_CHUNK_CHANGES;
                setInfo("distinct=" + distinct + " max=" + MAX_CHUNK_CHANGES + " windowMs=" + WINDOW_MS + " ticks=" + overTicks);
                innocence = Math.max(0.0, innocence - Math.min(1.0, over / 6.0) * 0.5);
            }
        } else {
            overTicks = Math.max(0, overTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
        }
    }
}
