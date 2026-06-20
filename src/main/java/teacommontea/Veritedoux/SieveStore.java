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

package teacommontea.veritedoux;

import org.bukkit.plugin.Plugin;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SieveStore {

    private static final int STONES_MAGIC = 0x53544E31;
    private static final int FLAGS_MAGIC  = 0x464C4731;

    private final File stonesFile;
    private final File flagsFile;
    private final Map<UUID, StoneRow> stones = new HashMap<>();
    private final Map<UUID, Integer> flagCount = new HashMap<>();
    private DataOutputStream flagsOut;

    private final java.util.concurrent.ExecutorService io =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Sieve-IO");
                t.setDaemon(true);
                return t;
            });

    private SieveStore(File stonesFile, File flagsFile) {
        this.stonesFile = stonesFile;
        this.flagsFile = flagsFile;
    }

    public static SieveStore open(Plugin plugin) throws Exception {
        plugin.getDataFolder().mkdirs();
        SieveStore s = new SieveStore(
                new File(plugin.getDataFolder(), "stones.bin"),
                new File(plugin.getDataFolder(), "flags.log"));
        s.loadStones();
        s.loadFlagCounts();
        s.flagsOut = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(s.flagsFile, true)));
        return s;
    }

    private void loadStones() {
        if (!stonesFile.isFile()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(stonesFile))) {
            if (in.readInt() != STONES_MAGIC) return;
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                UUID u = new UUID(in.readLong(), in.readLong());
                double score = in.readDouble();
                long firstSeen = in.readLong();
                boolean snapped = in.readBoolean();
                int penalty = in.readInt();
                stones.put(u, new StoneRow(score, firstSeen, snapped, penalty));
            }
        } catch (Exception ignored) {
        }
    }

    private void writeStones() {
        File tmp = new File(stonesFile.getParentFile(), stonesFile.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))) {
            out.writeInt(STONES_MAGIC);
            Map<UUID, StoneRow> snapshot;
            synchronized (stones) {
                snapshot = new HashMap<>(stones);
            }
            out.writeInt(snapshot.size());
            for (Map.Entry<UUID, StoneRow> e : snapshot.entrySet()) {
                out.writeLong(e.getKey().getMostSignificantBits());
                out.writeLong(e.getKey().getLeastSignificantBits());
                StoneRow r = e.getValue();
                out.writeDouble(r.score());
                out.writeLong(r.firstSeenMillis());
                out.writeBoolean(r.snapped());
                out.writeInt(r.penalty());
            }
        } catch (Exception ignored) {
            return;
        }
        tmp.renameTo(stonesFile);
    }

    public StoneRow loadStone(UUID player) {
        synchronized (stones) {
            return stones.get(player);
        }
    }

    public void saveStone(UUID player, double score, long firstSeenMillis, boolean snapped, int penalty) {
        synchronized (stones) {
            stones.put(player, new StoneRow(score, firstSeenMillis, snapped, penalty));
        }
        io.execute(this::writeStones);
    }

    public record StoneRow(double score, long firstSeenMillis, boolean snapped, int penalty) {}

    private void loadFlagCounts() {
        if (!flagsFile.isFile()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(flagsFile))) {
            while (true) {
                long hi, lo;
                try {
                    hi = in.readLong();
                } catch (EOFException eof) {
                    break;
                }
                lo = in.readLong();
                in.readLong();
                skipString(in);
                skipString(in);
                UUID u = new UUID(hi, lo);
                flagCount.merge(u, 1, Integer::sum);
            }
        } catch (Exception ignored) {
        }
    }

    public void record(UUID player, Sieve.Result category, String message) {
        synchronized (flagCount) {
            flagCount.merge(player, 1, Integer::sum);
        }
        String cat = category.name();
        String msg = message.length() > 512 ? message.substring(0, 512) : message;
        long at = System.currentTimeMillis();
        io.execute(() -> {
            try {
                flagsOut.writeLong(player.getMostSignificantBits());
                flagsOut.writeLong(player.getLeastSignificantBits());
                flagsOut.writeLong(at);
                writeString(flagsOut, cat);
                writeString(flagsOut, msg);
                flagsOut.flush();
            } catch (Exception ignored) {
            }
        });
    }

    public int count(UUID player) {
        synchronized (flagCount) {
            return flagCount.getOrDefault(player, 0);
        }
    }

    private static void writeString(DataOutputStream out, String s) throws Exception {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(b.length, 0xFFFF);
        out.writeShort(len);
        out.write(b, 0, len);
    }

    private static void skipString(DataInputStream in) throws Exception {
        int len = in.readUnsignedShort();
        int skipped = 0;
        while (skipped < len) {
            long s = in.skip(len - skipped);
            if (s <= 0) { in.readByte(); s = 1; }
            skipped += s;
        }
    }

    public void close() {
        io.shutdown();
        try {
            io.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        writeStones();
        try {
            if (flagsOut != null) flagsOut.close();
        } catch (Exception ignored) {
        }
    }
}
