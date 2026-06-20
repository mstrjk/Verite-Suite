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

package teacommontea.veritechasse.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import teacommontea.veritechasse.AntiCheat;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DetectionLog {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AntiCheat antiCheat;
    private final File dir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<UUID, PlayerRecord> cache = new ConcurrentHashMap<>();

    public DetectionLog(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
        this.dir = new File(antiCheat.getPlugin().getDataFolder(), "players");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void record(UUID uuid, String name, String kind, String check, String detail,
                       double score, String tier, String action) {
        long now = System.currentTimeMillis();
        Entry entry = new Entry();
        entry.time = now;
        entry.timeText = LocalDateTime.now().format(STAMP);
        entry.kind = kind;
        entry.check = check;
        entry.detail = detail == null ? "" : detail;
        entry.score = score;
        entry.tier = tier == null ? "" : tier;
        entry.action = action == null ? "" : action;
        try {
            Bukkit.getScheduler().runTaskAsynchronously(antiCheat.getPlugin(), () -> append(uuid, name, entry));
        } catch (IllegalStateException disabling) {
            append(uuid, name, entry);
        }
    }

    private void append(UUID uuid, String name, Entry entry) {
        PlayerRecord rec = cache.computeIfAbsent(uuid, k -> loadOrCreate(uuid, name));
        synchronized (rec) {
            rec.name = name;
            rec.lastUpdated = entry.time;
            rec.entries.add(entry);
            write(uuid, rec);
        }
    }

    private PlayerRecord loadOrCreate(UUID uuid, String name) {
        File file = new File(dir, uuid + ".json");
        if (file.isFile()) {
            try {
                String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                PlayerRecord rec = gson.fromJson(json, PlayerRecord.class);
                if (rec != null) {
                    if (rec.entries == null) {
                        rec.entries = new ArrayList<>();
                    }
                    return rec;
                }
            } catch (Exception ignored) {
            }
        }
        PlayerRecord rec = new PlayerRecord();
        rec.uuid = uuid.toString();
        rec.name = name;
        rec.firstSeen = System.currentTimeMillis();
        rec.entries = new ArrayList<>();
        return rec;
    }

    private void write(UUID uuid, PlayerRecord rec) {
        File file = new File(dir, uuid + ".json");
        try {
            Files.writeString(file.toPath(), gson.toJson(rec), StandardCharsets.UTF_8);
        } catch (IOException e) {
            antiCheat.getPlugin().getLogger().warning(
                    "Failed to write detection log for " + uuid + ": " + e.getMessage());
        }
    }

    public void flushAll() {
        for (Map.Entry<UUID, PlayerRecord> e : cache.entrySet()) {
            synchronized (e.getValue()) {
                write(e.getKey(), e.getValue());
            }
        }
    }

    static final class PlayerRecord {
        String uuid;
        String name;
        long firstSeen;
        long lastUpdated;
        List<Entry> entries = new ArrayList<>();
    }

    static final class Entry {
        long time;
        String timeText;
        String kind;
        String check;
        String detail;
        double score;
        String tier;
        String action;
    }
}
