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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class VeriteTrace {

    private final AntiCheat antiCheat;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final File file;
    private final AtomicLong seq = new AtomicLong(0L);

    private volatile boolean enabled;
    private volatile boolean echoConsole;
    private BufferedWriter writer;

    public VeriteTrace(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
        this.file = new File(antiCheat.getPlugin().getDataFolder(), "verite-trace.jsonl");
        org.bukkit.configuration.file.YamlConfiguration cfg = teacommontea.veritesauver.SauverConfig.yaml();
        this.enabled = cfg.getBoolean("trace.enabled", false);
        this.echoConsole = cfg.getBoolean("trace.console", false);
        if (enabled) {
            openWriter();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (value && !enabled) {
            openWriter();
        }
        this.enabled = value;
        if (!value) {
            closeWriter();
        }
    }

    public void setEchoConsole(boolean value) {
        this.echoConsole = value;
    }

    private synchronized void openWriter() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            writer = new BufferedWriter(new FileWriter(file, true));
        } catch (IOException e) {
            antiCheat.getPlugin().getLogger().warning("VeriteTrace open failed: " + e.getMessage());
            writer = null;
        }
    }

    private synchronized void closeWriter() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {
            }
            writer = null;
        }
    }

    public void event(String stage, UUID uuid, String player, Map<String, Object> fields) {
        if (!enabled) {
            return;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("seq", seq.incrementAndGet());
        row.put("ms", System.currentTimeMillis());
        row.put("stage", stage);
        row.put("uuid", uuid == null ? null : uuid.toString());
        row.put("player", player);
        if (fields != null) {
            row.putAll(fields);
        }
        String line = gson.toJson(row);
        synchronized (this) {
            if (writer != null) {
                try {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    antiCheat.getPlugin().getLogger().warning("VeriteTrace write failed: " + e.getMessage());
                }
            }
        }
        if (echoConsole) {
            antiCheat.getPlugin().getLogger().info("[trace] " + line);
        }
    }

    public static Map<String, Object> fields(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    public void shutdown() {
        closeWriter();
    }
}
