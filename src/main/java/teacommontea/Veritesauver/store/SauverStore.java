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

package teacommontea.veritesauver.store;

import org.bukkit.plugin.Plugin;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class SauverStore {

    private static final int MAGIC = 0x53565231;

    private static final byte T_STRING = 1;
    private static final byte T_LONG   = 2;
    private static final byte T_INT    = 3;
    private static final byte T_BOOL   = 4;
    private static final byte T_DEC    = 5;

    private final File file;
    private final Map<String, TreeMap<String, Object>> scopes = new ConcurrentHashMap<>();

    private final java.util.concurrent.ExecutorService io =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Sauver-IO");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean dirty;

    private SauverStore(File file) {
        this.file = file;
    }

    public static SauverStore open(Plugin plugin) throws Exception {
        return open(plugin, "sauver.bin");
    }

    public static SauverStore open(Plugin plugin, String fileName) throws Exception {
        plugin.getDataFolder().mkdirs();
        SauverStore s = new SauverStore(new File(plugin.getDataFolder(), fileName));
        s.load();
        return s;
    }

    public Scope scope(String name) {
        return new Scope(this, name);
    }

    void set(String scope, String key, Object value) {
        if (value == null) {
            delete(scope, key);
            return;
        }
        scopes.computeIfAbsent(scope, k -> new TreeMap<>()).put(key, value);
        markDirty();
    }

    void delete(String scope, String key) {
        TreeMap<String, Object> m = scopes.get(scope);
        if (m == null) {
            return;
        }
        if (m.remove(key) != null) {
            if (m.isEmpty()) {
                scopes.remove(scope);
            }
            markDirty();
        }
    }

    Object get(String scope, String key) {
        TreeMap<String, Object> m = scopes.get(scope);
        return m == null ? null : m.get(key);
    }

    List<Scope.Entry> entries(String scope) {
        TreeMap<String, Object> m = scopes.get(scope);
        if (m == null) {
            return Collections.emptyList();
        }
        List<Scope.Entry> out = new ArrayList<>(m.size());
        for (Map.Entry<String, Object> e : m.entrySet()) {
            out.add(new Scope.Entry(e.getKey(), e.getValue()));
        }
        return out;
    }

    private void markDirty() {
        dirty = true;
        io.execute(this::flushIfDirty);
    }

    private synchronized void flushIfDirty() {
        if (!dirty) {
            return;
        }
        dirty = false;

        File tmp = new File(file.getAbsolutePath() + ".part");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))) {
            out.writeInt(MAGIC);
            out.writeInt(scopes.size());
            for (Map.Entry<String, TreeMap<String, Object>> s : scopes.entrySet()) {
                writeString(out, s.getKey());
                out.writeInt(s.getValue().size());
                for (Map.Entry<String, Object> kv : s.getValue().entrySet()) {
                    writeString(out, kv.getKey());
                    writeValue(out, kv.getValue());
                }
            }
        } catch (Exception e) {
            dirty = true;
            return;
        }
        try {
            java.nio.file.Files.move(tmp.toPath(), file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            dirty = true;
        }
    }

    private void load() {
        if (!file.isFile()) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (in.readInt() != MAGIC) {
                return;
            }
            int scopeCount = in.readInt();
            for (int i = 0; i < scopeCount; i++) {
                String scope = readString(in);
                int keyCount = in.readInt();
                TreeMap<String, Object> m = new TreeMap<>();
                for (int j = 0; j < keyCount; j++) {
                    String key = readString(in);
                    m.put(key, readValue(in));
                }
                if (!m.isEmpty()) {
                    scopes.put(scope, m);
                }
            }
        } catch (EOFException ignored) {

        } catch (Exception ignored) {
        }
    }

    public void shutdown() {
        flushIfDirty();
        io.shutdown();
    }

    private static void writeString(DataOutputStream out, String s) throws java.io.IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
    }

    private static String readString(DataInputStream in) throws java.io.IOException {
        int len = in.readInt();
        byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private static void writeValue(DataOutputStream out, Object v) throws java.io.IOException {
        if (v instanceof Boolean b) {
            out.writeByte(T_BOOL);
            out.writeBoolean(b);
        } else if (v instanceof Integer i) {
            out.writeByte(T_INT);
            out.writeInt(i);
        } else if (v instanceof Long l) {
            out.writeByte(T_LONG);
            out.writeLong(l);
        } else if (v instanceof BigDecimal d) {
            out.writeByte(T_DEC);
            writeString(out, d.toPlainString());
        } else {
            out.writeByte(T_STRING);
            writeString(out, String.valueOf(v));
        }
    }

    private static Object readValue(DataInputStream in) throws java.io.IOException {
        byte tag = in.readByte();
        return switch (tag) {
            case T_BOOL -> in.readBoolean();
            case T_INT -> in.readInt();
            case T_LONG -> in.readLong();
            case T_DEC -> new BigDecimal(readString(in));
            default -> readString(in);
        };
    }
}
