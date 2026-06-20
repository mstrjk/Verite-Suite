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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import teacommontea.veritechasse.AntiCheat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ReplayStore {

    private final AntiCheat antiCheat;
    private final File dir;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public ReplayStore(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
        this.dir = new File(antiCheat.getPlugin().getDataFolder(), "replays");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void save(ReplayRecord record) {
        File file = new File(dir, record.id + ".vrep");
        try {
            Files.writeString(file.toPath(), gson.toJson(record), StandardCharsets.UTF_8);
        } catch (Exception e) {
            antiCheat.getPlugin().getLogger().warning("[Replay] failed to save " + record.id + ": " + e.getMessage());
        }
    }

    public ReplayRecord load(String id) {
        File file = new File(dir, id + ".vrep");
        if (!file.isFile()) {
            return null;
        }
        try {
            return gson.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8), ReplayRecord.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<ReplayRecord> forPlayer(String name) {
        List<ReplayRecord> out = new ArrayList<>();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".vrep"));
        if (files == null) {
            return out;
        }
        for (File f : files) {
            try {
                ReplayRecord r = gson.fromJson(Files.readString(f.toPath(), StandardCharsets.UTF_8), ReplayRecord.class);
                if (r != null && r.suspectName != null && r.suspectName.equalsIgnoreCase(name)) {
                    out.add(r);
                }
            } catch (Exception ignored) {
            }
        }
        out.sort((a, b) -> Long.compare(b.savedAt, a.savedAt));
        return out;
    }

    public ReplayRecord latestFor(String name) {
        List<ReplayRecord> all = forPlayer(name);
        return all.isEmpty() ? null : all.get(0);
    }
}
