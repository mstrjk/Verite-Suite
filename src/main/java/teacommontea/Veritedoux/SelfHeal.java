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

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SelfHeal {

    private SelfHeal() {}

    public static void healSettings(Plugin plugin) {
        try {
            doHealSettings(plugin);
        } catch (Exception e) {
            plugin.getLogger().warning("[Verite] settings.yml self-heal skipped: " + e.getMessage());
        }
    }

    private static void doHealSettings(Plugin plugin) throws Exception {
        File deployed = new File(plugin.getDataFolder(), "settings.yml");
        if (!deployed.isFile()) return;

        String bundledText = readResource(plugin, "sieve/settings.yml");
        if (bundledText == null) return;

        String deployedText = new String(Files.readAllBytes(deployed.toPath()), StandardCharsets.UTF_8);
        Merge merged = mergeSettings(bundledText, deployedText);
        if (merged.added == 0) return;

        Files.write(deployed.toPath(), merged.text.getBytes(StandardCharsets.UTF_8));
        plugin.getLogger().info("[Verite] settings.yml self-heal added " + merged.added
                + " missing key block" + (merged.added == 1 ? "" : "s") + " (with their comments).");
    }

    static final class Merge {
        final String text;
        final int added;
        Merge(String text, int added) { this.text = text; this.added = added; }
    }

    static Merge mergeSettings(String bundledText, String deployedText) {
        List<String> bundledLines = splitLines(bundledText);
        List<Entry> bundledEntries = parse(bundledLines);

        List<String> deployedLines = new ArrayList<>(splitLines(deployedText));
        Set<String> deployedPaths = paths(parse(deployedLines));

        List<Entry> toAdd = new ArrayList<>();
        for (Entry e : bundledEntries) {
            if (deployedPaths.contains(e.path)) continue;
            if (coveredBy(e.path, toAdd)) continue;
            toAdd.add(e);
        }
        if (toAdd.isEmpty()) return new Merge(deployedText, 0);

        List<String> out = new ArrayList<>(deployedLines);
        int added = 0;
        for (Entry e : toAdd) {
            List<String> block = blockText(bundledLines, e);
            int at = insertionPoint(out, e, bundledEntries);
            if (at < 0) {
                if (!out.isEmpty() && !out.get(out.size() - 1).trim().isEmpty()) out.add("");
                out.addAll(block);
            } else {
                out.addAll(at, block);
            }
            added++;
        }
        return new Merge(String.join("\n", out) + "\n", added);
    }

    private static final class Entry {
        final String path;
        final int indent;
        final int keyLine;
        final int commentStart;
        Entry(String path, int indent, int keyLine, int commentStart) {
            this.path = path; this.indent = indent; this.keyLine = keyLine; this.commentStart = commentStart;
        }
        String pathName() {
            int dot = path.lastIndexOf('.');
            return dot < 0 ? path : path.substring(dot + 1);
        }
    }

    private static List<Entry> parse(List<String> lines) {
        List<Entry> entries = new ArrayList<>();
        List<int[]> stack = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.startsWith("-")) continue;

            int colon = keyColon(line);
            if (colon < 0) continue;

            int indent = indentOf(line);
            String name = line.substring(indent, colon).trim();
            if (name.isEmpty()) continue;

            while (!stack.isEmpty() && stack.get(stack.size() - 1)[0] >= indent) {
                stack.remove(stack.size() - 1);
            }
            StringBuilder pathB = new StringBuilder();
            for (int[] anc : stack) {
                pathB.append(entries.get(anc[1]).pathName()).append('.');
            }
            pathB.append(name);
            String path = pathB.toString();

            int commentStart = commentStartFor(lines, i);
            Entry e = new Entry(path, indent, i, commentStart);
            entries.add(e);
            stack.add(new int[]{indent, entries.size() - 1});
        }
        return entries;
    }

    private static int commentStartFor(List<String> lines, int keyLine) {
        int start = keyLine;
        for (int j = keyLine - 1; j >= 0; j--) {
            String t = lines.get(j).trim();
            if (t.startsWith("#")) { start = j; continue; }
            break;
        }
        return start;
    }

    private static List<String> blockText(List<String> lines, Entry e) {
        List<String> block = new ArrayList<>();
        int end = blockEnd(lines, e);
        for (int j = e.commentStart; j < end; j++) block.add(lines.get(j));
        return block;
    }

    private static int insertionPoint(List<String> out, Entry e, List<Entry> bundledEntries) {
        List<Entry> current = parse(out);
        String parentPath = parentOf(e.path);

        List<Entry> siblings = new ArrayList<>();
        for (Entry b : bundledEntries) if (eq(parentOf(b.path), parentPath)) siblings.add(b);
        int self = -1;
        for (int i = 0; i < siblings.size(); i++) if (siblings.get(i).path.equals(e.path)) { self = i; break; }

        for (int i = self + 1; i < siblings.size(); i++) {
            Entry deployed = find(current, siblings.get(i).path);
            if (deployed != null) return deployed.commentStart;
        }
        for (int i = self - 1; i >= 0; i--) {
            Entry deployed = find(current, siblings.get(i).path);
            if (deployed != null) return blockEnd(out, deployed);
        }
        if (parentPath != null) {
            Entry parent = find(current, parentPath);
            if (parent != null) return parent.keyLine + 1;
        }
        return -1;
    }

    private static int blockEnd(List<String> lines, Entry e) {
        int end = e.keyLine + 1;
        int j = e.keyLine + 1;
        while (j < lines.size()) {
            String t = lines.get(j).trim();
            if (t.isEmpty() || t.startsWith("#")) {
                int k = j;
                while (k < lines.size()) {
                    String tk = lines.get(k).trim();
                    if (tk.isEmpty() || tk.startsWith("#")) { k++; continue; }
                    break;
                }
                if (k >= lines.size() || indentOf(lines.get(k)) <= e.indent) break;
                j = k;
                continue;
            }
            if (indentOf(lines.get(j)) <= e.indent) break;
            end = j + 1;
            j++;
        }
        return end;
    }

    private static String parentOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? null : path.substring(0, dot);
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static Entry find(List<Entry> entries, String path) {
        for (Entry e : entries) if (e.path.equals(path)) return e;
        return null;
    }

    private static boolean coveredBy(String path, List<Entry> chosen) {
        for (Entry e : chosen) {
            if (path.equals(e.path)) return true;
            if (path.startsWith(e.path + ".")) return true;
        }
        return false;
    }

    private static Set<String> paths(List<Entry> entries) {
        Set<String> s = new LinkedHashSet<>();
        for (Entry e : entries) s.add(e.path);
        return s;
    }

    public static void healEve(Plugin plugin, String[] eveConfigs) {
        try {
            doHealEve(plugin, eveConfigs);
        } catch (Exception e) {
            plugin.getLogger().warning("[Verite] config .eve self-heal skipped: " + e.getMessage());
        }
    }

    private static void doHealEve(Plugin plugin, String[] eveConfigs) throws Exception {
        for (String config : eveConfigs) {
            File deployed = new File(plugin.getDataFolder(), config);
            if (!deployed.isFile()) continue;

            String deployedText = new String(Files.readAllBytes(deployed.toPath()), StandardCharsets.UTF_8);

            for (String version : updateVersions(plugin, config)) {
                String banner = "REALM " + bannerFor(version);
                if (deployedText.contains(banner)) continue;

                String diff = readResource(plugin, "sieve/updates/" + version + "/" + config);
                if (diff == null || diff.isBlank()) continue;

                StringBuilder append = new StringBuilder();
                if (!deployedText.endsWith("\n")) append.append('\n');
                append.append('\n').append(banner).append('\n').append(diff.strip()).append('\n');

                Files.write(deployed.toPath(), append.toString().getBytes(StandardCharsets.UTF_8),
                        java.nio.file.StandardOpenOption.APPEND);
                deployedText = deployedText + append;
                plugin.getLogger().info("[Verite] " + config + " self-heal applied eve update " + version + ".");
            }
        }
    }

    private static String bannerFor(String version) {
        return "____ added during " + version + " eve update ____";
    }

    private static List<String> updateVersions(Plugin plugin, String config) {
        List<String> versions = new ArrayList<>();
        String manifest = readResource(plugin, "sieve/updates/manifest.txt");
        if (manifest == null) return versions;
        for (String line : manifest.split("\n")) {
            String v = line.trim();
            if (!v.isEmpty() && !v.startsWith("#")) versions.add(v);
        }
        return versions;
    }

    private static String readResource(Plugin plugin, String name) {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        for (String s : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) out.add(s);
        if (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) out.remove(out.size() - 1);
        return out;
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return i;
    }

    private static int keyColon(String line) {
        boolean inq = false;
        char q = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inq) { if (c == q) inq = false; continue; }
            if (c == '"' || c == '\'') { inq = true; q = c; continue; }
            if (c == '#') return -1;
            if (c == ':') {
                if (i + 1 >= line.length() || line.charAt(i + 1) == ' ') return i;
            }
        }
        return -1;
    }
}
