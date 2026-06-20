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

package teacommontea.veritechasse.replay;

import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ReplayRecorder {

    private static final int WINDOW_TICKS = 60 * 20;
    private static final int CAPTURE_INTERVAL = 2;

    private final VeritePlayer player;
    private final Deque<ReplayFrame> frames = new ArrayDeque<>();
    private final Deque<ReplayEvent> events = new ArrayDeque<>();

    public ReplayRecorder(VeritePlayer player) {
        this.player = player;
    }

    public void tick(Player bukkit) {
        int now = player.currentTick();
        prune(now);
        if (now % CAPTURE_INTERVAL == 0) {
            frames.addLast(capture(bukkit, now));
        }
    }

    public void event(ReplayEvent.Type type, int targetId, int x, int y, int z, String detail) {
        int now = player.currentTick();
        events.addLast(new ReplayEvent(now, type, targetId, x, y, z, detail));
        prune(now);
    }

    public List<ReplayFrame> framesBetween(int fromTick, int toTick) {
        List<ReplayFrame> out = new ArrayList<>();
        for (ReplayFrame f : frames) {
            if (f.tick >= fromTick && f.tick <= toTick) {
                out.add(f);
            }
        }
        return out;
    }

    public List<ReplayEvent> eventsBetween(int fromTick, int toTick) {
        List<ReplayEvent> out = new ArrayList<>();
        for (ReplayEvent e : events) {
            if (e.tick >= fromTick && e.tick <= toTick) {
                out.add(e);
            }
        }
        return out;
    }

    private void prune(int now) {
        int cutoff = now - WINDOW_TICKS;
        while (!frames.isEmpty() && frames.peekFirst().tick < cutoff) {
            frames.pollFirst();
        }
        while (!events.isEmpty() && events.peekFirst().tick < cutoff) {
            events.pollFirst();
        }
    }

    private ReplayFrame capture(Player bukkit, int now) {
        Location loc = bukkit.getLocation();
        int flags = 0;
        if (player.snapshot.valid && player.snapshot.bukkitOnGround) flags |= ReplayFrame.FLAG_ON_GROUND;
        if (bukkit.isSprinting()) flags |= ReplayFrame.FLAG_SPRINTING;
        if (bukkit.isSneaking()) flags |= ReplayFrame.FLAG_SNEAKING;
        if (bukkit.isSwimming()) flags |= ReplayFrame.FLAG_SWIMMING;
        if (bukkit.isGliding()) flags |= ReplayFrame.FLAG_GLIDING;
        if (bukkit.isInWater()) flags |= ReplayFrame.FLAG_IN_WATER;
        return new ReplayFrame(now, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), flags);
    }
}
