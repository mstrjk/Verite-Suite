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

import teacommontea.veritechasse.AntiCheat;
import teacommontea.veritechasse.player.VeritePlayer;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReplayManager implements Listener {

    private static final int POST_TICKS = 5 * 20;
    private static final int PRE_TICKS = 10 * 20;

    private static final java.time.format.DateTimeFormatter ID_FMT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").withZone(java.time.ZoneOffset.UTC);

    private final AntiCheat antiCheat;
    private final ReplayStore store;
    private final NamespacedKey controlKey;
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ReplayPlayback> playing = new ConcurrentHashMap<>();

    public ReplayManager(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
        this.store = new ReplayStore(antiCheat);
        this.controlKey = new NamespacedKey(antiCheat.getPlugin(), "replay_ctrl");
    }

    public ReplayStore store() {
        return store;
    }

    public NamespacedKey controlKey() {
        return controlKey;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ReplayPlayback playback = playing.get(event.getPlayer().getUniqueId());
        if (playback == null) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String action = item.getItemMeta().getPersistentDataContainer().get(controlKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        event.setCancelled(true);
        playback.control(action);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (playing.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopPlayback(event.getPlayer());
    }

    public void arm(VeritePlayer player, String check, String evidence) {
        try {
            UUID u = player.getUuid();
            if (!pending.add(u)) {
                return;
            }
            int flagTick = player.currentTick();
            String worldName = worldOf(u);
            Bukkit.getScheduler().runTaskLater(antiCheat.getPlugin(),
                    () -> finalizeCapture(player, check, evidence, worldName, flagTick), POST_TICKS);
        } catch (Throwable t) {
            pending.remove(player.getUuid());
            antiCheat.getPlugin().getLogger().warning("[Replay] arm failed: " + t.getMessage());
        }
    }

    private String worldOf(UUID u) {
        Player p = Bukkit.getPlayer(u);
        if (p != null && p.getWorld() != null) {
            return p.getWorld().getName();
        }
        return "world";
    }

    private void finalizeCapture(VeritePlayer player, String check, String evidence,
                                String worldName, int flagTick) {
        try {
            UUID u = player.getUuid();
            pending.remove(u);

            ReplayRecorder recorder = player.getReplay();
            List<ReplayFrame> frames = recorder.framesBetween(flagTick - PRE_TICKS, flagTick + POST_TICKS);
            if (frames.isEmpty()) {
                return;
            }
            List<ReplayEvent> events = recorder.eventsBetween(flagTick - PRE_TICKS, flagTick + POST_TICKS);

            Player bukkit = Bukkit.getPlayer(u);
            String name = bukkit != null ? bukkit.getName() : u.toString();
            String id = name + "-" + ID_FMT.format(java.time.Instant.now()) + "-utc";
            String[] skin = skinOf(bukkit);

            ReplayRecord record = new ReplayRecord();
            record.id = id;
            record.suspect = u.toString();
            record.suspectName = name;
            record.savedAt = System.currentTimeMillis();
            record.flagTick = flagTick;
            record.check = check;
            record.evidence = evidence;
            record.worldName = worldName;
            record.skinValue = skin[0];
            record.skinSignature = skin[1];
            record.frames = new ArrayList<>();
            for (ReplayFrame f : frames) {
                ReplayRecord.Frame rf = new ReplayRecord.Frame();
                rf.t = f.tick - flagTick;
                rf.x = f.x;
                rf.y = f.y;
                rf.z = f.z;
                rf.yaw = f.yaw;
                rf.pitch = f.pitch;
                rf.flags = f.flags;
                record.frames.add(rf);
            }
            record.events = new ArrayList<>();
            for (ReplayEvent e : events) {
                ReplayRecord.Event re = new ReplayRecord.Event();
                re.t = e.tick - flagTick;
                re.type = e.type.name();
                re.target = e.targetId;
                re.x = e.x;
                re.y = e.y;
                re.z = e.z;
                re.detail = e.detail;
                record.events.add(re);
            }

            store.save(record);
            antiCheat.getPlugin().getLogger().info("[Replay] saved " + id + " frames=" + frames.size());
        } catch (Throwable t) {
            pending.remove(player.getUuid());
            antiCheat.getPlugin().getLogger().warning("[Replay] capture failed: " + t.getMessage());
        }
    }

    private String[] skinOf(Player bukkit) {
        if (bukkit == null) {
            return new String[]{"", ""};
        }
        try {
            for (var prop : bukkit.getPlayerProfile().getProperties()) {
                if (prop.getName().equals("textures")) {
                    return new String[]{prop.getValue(), prop.getSignature() == null ? "" : prop.getSignature()};
                }
            }
        } catch (Throwable ignored) {
        }
        return new String[]{"", ""};
    }

    public boolean play(Player viewer, ReplayRecord record) {
        if (record == null) {
            return false;
        }
        stopPlayback(viewer);
        ReplayPlayback playback = new ReplayPlayback(antiCheat, viewer, record);
        playing.put(viewer.getUniqueId(), playback);
        return playback.start();
    }

    public boolean stopPlayback(Player viewer) {
        ReplayPlayback playback = playing.get(viewer.getUniqueId());
        if (playback != null) {
            playback.stop();
            return true;
        }
        return false;
    }

    public void forget(UUID viewer) {
        playing.remove(viewer);
    }
}
