package teacommontea.eve;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class Eve implements AutoCloseable {

    private static boolean NATIVE_OK;
    private static Throwable NATIVE_ERR;

    private static final String CAP_PAIR = "";
    private static final String CAP_KV = "";

    static {
        try {
            loadNative();
            NATIVE_OK = true;
        } catch (Throwable t) {
            NATIVE_OK = false;
            NATIVE_ERR = t;
        }
    }

    public static boolean nativeAvailable() {
        return NATIVE_OK;
    }

    public static Throwable nativeError() {
        return NATIVE_ERR;
    }

    private static void loadNative() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String osDir;
        String libName;
        if (os.contains("win")) {
            osDir = "windows";
            libName = "eve.dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osDir = "macos";
            libName = "libeve.dylib";
        } else {
            osDir = isMusl() ? "linux-musl" : "linux";
            libName = "libeve.so";
        }

        String archDir;
        if (arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64")) {
            archDir = "x86-64";
        } else if (arch.equals("aarch64") || arch.equals("arm64")) {
            archDir = "aarch64";
        } else {
            archDir = arch;
        }

        String resource = "/native/" + osDir + "-" + archDir + "/" + libName;
        try (InputStream in = Eve.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("no bundled native lib at " + resource);
            }
            Path tmp = Files.createTempFile("eve-", "-" + libName);
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            System.load(tmp.toAbsolutePath().toString());
        }
    }

    private static boolean isMusl() {
        try {
            Path p = Path.of("/proc/self/maps");
            if (Files.exists(p)) {
                String maps = Files.readString(p);
                return maps.contains("musl") || maps.contains("ld-musl");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private long handle;

    private Eve(long handle) {
        this.handle = handle;
    }

    public static Eve parse(String source) {
        long h = nParse(source);
        if (h == 0) {
            throw new IllegalArgumentException("EVE parse failed");
        }
        return new Eve(h);
    }

    public int ruleCount() {
        return nRuleCount(handle);
    }

    public String ruleName(int i) {
        return nRuleName(handle, i);
    }

    public String ruleRealm(int i) {
        return nRuleRealm(handle, i);
    }

    public boolean matches(int rule, String candidate) {
        return nMatches(handle, rule, candidate, candidate);
    }

    public boolean matches(int rule, String candidate, String fullLine) {
        return nMatches(handle, rule, candidate, fullLine);
    }

    public int firstMatch(String candidate, String fullLine) {
        return nFirstMatch(handle, candidate, fullLine);
    }

    public List<Match> scan(String candidate) {
        return scan(candidate, candidate);
    }

    public List<Match> scan(String candidate, String fullLine) {
        String packed = nScan(handle, candidate, fullLine);
        List<Match> out = new ArrayList<>();
        if (packed == null || packed.isEmpty()) {
            return out;
        }
        for (String row : packed.split("\n")) {
            String[] col = row.split("\t", -1);
            if (col.length < 4) continue;
            int rule = Integer.parseInt(col[0]);
            java.util.Map<String, Boolean> flags = new java.util.LinkedHashMap<>();
            if (!col[3].isEmpty()) {
                for (String f : col[3].split(" ")) {
                    int eq = f.lastIndexOf('=');
                    if (eq >= 0) flags.put(f.substring(0, eq), f.charAt(eq + 1) == '1');
                }
            }
            java.util.Map<String, String> caps = new java.util.LinkedHashMap<>();
            if (col.length >= 5 && !col[4].isEmpty()) {
                for (String pair : col[4].split(CAP_PAIR)) {
                    int sep = pair.indexOf(CAP_KV);
                    if (sep >= 0) caps.put(pair.substring(0, sep), pair.substring(sep + CAP_KV.length()));
                }
            }
            out.add(new Match(rule, col[1], col[2], flags, caps));
        }
        return out;
    }

    public record Match(int rule, String name, String realm,
                        java.util.Map<String, Boolean> flags,
                        java.util.Map<String, String> captures) {
        public boolean flag(String name, boolean dflt) {
            return flags.getOrDefault(name, dflt);
        }
        public String capture(String name) {
            return captures.get(name);
        }
    }

    public List<Flag> flags(int rule) {
        String packed = nFlags(handle, rule);
        List<Flag> out = new ArrayList<>();
        if (packed == null || packed.isEmpty()) {
            return out;
        }
        for (String f : packed.split(" ")) {
            int eq = f.lastIndexOf('=');
            if (eq < 0) continue;
            out.add(new Flag(f.substring(0, eq), f.charAt(eq + 1) == '1'));
        }
        return out;
    }

    public record Flag(String name, boolean value) {}

    public record ConcreteWord(String word, java.util.Map<String, Boolean> flags) {
        public boolean flag(String name, boolean dflt) {
            return flags.getOrDefault(name, dflt);
        }
    }

    public List<ConcreteWord> concreteWords() {
        String packed = nConcreteWords(handle);
        List<ConcreteWord> out = new ArrayList<>();
        if (packed == null || packed.isEmpty()) {
            return out;
        }
        for (String row : packed.split("\n")) {
            int tab = row.indexOf('\t');
            if (tab < 0) continue;
            String word = row.substring(0, tab);
            java.util.Map<String, Boolean> flags = new java.util.LinkedHashMap<>();
            String fstr = row.substring(tab + 1);
            if (!fstr.isEmpty()) {
                for (String f : fstr.split(" ")) {
                    int eq = f.lastIndexOf('=');
                    if (eq >= 0) flags.put(f.substring(0, eq), f.charAt(eq + 1) == '1');
                }
            }
            out.add(new ConcreteWord(word, flags));
        }
        return out;
    }

    @Override
    public void close() {
        if (handle != 0) {
            nFree(handle);
            handle = 0;
        }
    }

    private static native long nParse(String source);
    private static native void nFree(long handle);
    private static native int nRuleCount(long handle);
    private static native String nRuleName(long handle, int i);
    private static native String nRuleRealm(long handle, int i);
    private static native boolean nMatches(long handle, int rule, String candidate, String fullLine);
    private static native int nFirstMatch(long handle, String candidate, String fullLine);
    private static native String nScan(long handle, String candidate, String fullLine);
    private static native String nFlags(long handle, int rule);
    private static native String nConcreteWords(long handle);
}
