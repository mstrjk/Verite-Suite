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

import teacommontea.veritechasse.net.GameMode;
import teacommontea.veritechasse.net.Vec3d;

import teacommontea.veritechasse.AntiCheat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReplayPlayback {

    private static final AtomicInteger ENTITY_IDS = new AtomicInteger(1_500_000);
    private static final double SPEED_MIN = 0.25;
    private static final double SPEED_MAX = 4.0;

    private final AntiCheat antiCheat;
    private final Player viewer;
    private final ReplayRecord record;

    private final int entityId = ENTITY_IDS.incrementAndGet();
    private final UUID npcUuid = UUID.randomUUID();

    private Location returnLocation;
    private org.bukkit.GameMode returnGameMode;
    private boolean returnAllowFlight;
    private boolean returnFlying;
    private ItemStack[] returnInventory;
    private int returnHeldSlot;

    private World world;
    private double cursor;
    private int minT;
    private int maxT;
    private double speed = 1.0;
    private int direction = 1;
    private boolean paused;
    private boolean lock;
    private int lastFiredTick = Integer.MIN_VALUE;
    private double[] lastState;
    private int taskId = -1;
    private boolean stopped;

    public ReplayPlayback(AntiCheat antiCheat, Player viewer, ReplayRecord record) {
        this.antiCheat = antiCheat;
        this.viewer = viewer;
        this.record = record;
    }

    public boolean start() {
        if (record.frames == null || record.frames.isEmpty()) {
            return false;
        }
        world = Bukkit.getWorld(record.worldName);
        if (world == null) {
            viewer.sendMessage(antiCheat.messages().prefixed("<red>That replay's world is not loaded."));
            return false;
        }
        minT = record.frames.get(0).t;
        maxT = record.frames.get(record.frames.size() - 1).t;
        cursor = minT;

        returnLocation = viewer.getLocation();
        returnGameMode = viewer.getGameMode();
        returnAllowFlight = viewer.getAllowFlight();
        returnFlying = viewer.isFlying();
        returnInventory = viewer.getInventory().getContents();
        returnHeldSlot = viewer.getInventory().getHeldItemSlot();

        ReplayRecord.Frame f0 = record.frames.get(0);
        double yawRad = Math.toRadians(f0.yaw);
        Location view = new Location(world,
                f0.x + Math.sin(yawRad) * 4.0, f0.y + 1.5, f0.z - Math.cos(yawRad) * 4.0,
                f0.yaw, 15f);
        viewer.setGameMode(org.bukkit.GameMode.ADVENTURE);
        viewer.teleport(view);
        viewer.setAllowFlight(true);
        viewer.setFlying(true);
        giveControls();

        spawn(f0);
        taskId = Bukkit.getScheduler().runTaskTimer(antiCheat.getPlugin(), this::tick, 1L, 1L).getTaskId();
        return true;
    }

    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        antiCheat.getReplayManager().forget(viewer.getUniqueId());
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        if (viewer.isOnline()) {
            if (returnInventory != null) {
                viewer.getInventory().setContents(returnInventory);
                viewer.getInventory().setHeldItemSlot(returnHeldSlot);
            }
            viewer.setGameMode(returnGameMode);
            viewer.setAllowFlight(returnAllowFlight);
            viewer.setFlying(returnFlying);
            if (returnLocation != null) {
                viewer.teleport(returnLocation);
            }
        }
    }

    public void control(String action) {
        switch (action) {
            case "REVERSE" -> {
                direction = -1;
                paused = false;
            }
            case "PLAY" -> {
                direction = 1;
                paused = false;
            }
            case "PAUSE" -> paused = !paused;
            case "SLOWER" -> speed = Math.max(SPEED_MIN, speed / 2.0);
            case "FASTER" -> speed = Math.min(SPEED_MAX, speed * 2.0);
            case "LOCK" -> lock = !lock;
            case "EXIT" -> {
                stop();
                return;
            }
            default -> {
                return;
            }
        }
        status();
    }

    private void tick() {
        if (!viewer.isOnline()) {
            stop();
            return;
        }

        if (!paused) {
            cursor += direction * speed;
            if (cursor > maxT) {
                cursor = minT;
            } else if (cursor < minT) {
                cursor = maxT;
            }
        }

        int t = (int) Math.round(cursor);
        double[] state = interpolate(t);
        lastState = state;

        if (t != lastFiredTick) {
            fireEvents(t);
            lastFiredTick = t;
        }

        if (lock) {
            double yawRad = Math.toRadians(state[3]);
            Location follow = new Location(world,
                    state[0] + Math.sin(yawRad) * 4.0, state[1] + 1.5, state[2] - Math.cos(yawRad) * 4.0,
                    (float) state[3], 15f);
            viewer.teleport(follow);
        }
    }

    private void fireEvents(int t) {
        if (record.events == null) {
            return;
        }
        for (ReplayRecord.Event e : record.events) {
            if (e.t == t && ("SWING".equals(e.type) || "ATTACK".equals(e.type))) {

            }
        }
    }

    private double[] interpolate(int t) {
        List<ReplayRecord.Frame> frames = record.frames;
        ReplayRecord.Frame lo = frames.get(0);
        ReplayRecord.Frame hi = frames.get(frames.size() - 1);
        for (int i = 0; i < frames.size() - 1; i++) {
            if (frames.get(i).t <= t && frames.get(i + 1).t >= t) {
                lo = frames.get(i);
                hi = frames.get(i + 1);
                break;
            }
        }
        double span = hi.t - lo.t;
        double a = span <= 0 ? 0 : (t - lo.t) / span;
        return new double[]{
                lo.x + (hi.x - lo.x) * a,
                lo.y + (hi.y - lo.y) * a,
                lo.z + (hi.z - lo.z) * a,
                lerpAngle(lo.yaw, hi.yaw, a),
                lo.pitch + (hi.pitch - lo.pitch) * a};
    }

    private double lerpAngle(float from, float to, double a) {
        float diff = to - from;
        while (diff < -180f) diff += 360f;
        while (diff > 180f) diff -= 360f;
        return from + diff * a;
    }

    private void giveControls() {
        viewer.getInventory().clear();
        viewer.getInventory().setItem(0, control(Material.ARROW, "<red>◄ Reverse", "REVERSE"));
        viewer.getInventory().setItem(1, control(Material.CLOCK, "<#B1C7F0>Slow Down", "SLOWER"));
        viewer.getInventory().setItem(2, control(Material.SUGAR, "<#B1C7F0>Speed Up", "FASTER"));
        viewer.getInventory().setItem(3, control(Material.ICE, "<white>Pause", "PAUSE"));
        viewer.getInventory().setItem(4, control(Material.LIME_DYE, "<green>► Play", "PLAY"));
        viewer.getInventory().setItem(5, control(Material.TRIPWIRE_HOOK, "<#B1C7F0>Lock Camera", "LOCK"));
        viewer.getInventory().setItem(8, control(Material.BARRIER, "<red>Exit", "EXIT"));
        viewer.getInventory().setHeldItemSlot(4);
    }

    private ItemStack control(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(antiCheat.messages().parse(name)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(
                    antiCheat.getReplayManager().controlKey(), PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void status() {
        String dir = paused ? "<white>⏸ Paused" : (direction < 0 ? "<red>◄ Reverse" : "<green>► Play");
        String lockTag = lock ? " <#B1C7F0>[Lock]" : "";
        viewer.sendActionBar(antiCheat.messages().parse(
                dir + " <white>x<white>" + speed + lockTag));
    }

    private void spawn(ReplayRecord.Frame f0) {

    }

    private void send(Object packet) {

    }
}
