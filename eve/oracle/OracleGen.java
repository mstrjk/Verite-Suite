import teacommontea.veritedoux.SieveToken;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OracleGen {

    private static String jsonValue(String s, String key) {
        int k = s.indexOf("\"" + key + "\"");
        if (k < 0) return null;
        int open = s.indexOf('"', s.indexOf(':', k) + 1);
        if (open < 0) return null;
        int close = s.indexOf('"', open + 1);
        if (close <= open) return null;
        return s.substring(open + 1, close);
    }

    private static String extractPattern(String line) {
        String s = line.trim();
        if (s.isEmpty() || s.startsWith("#") || s.contains("\"function\"")) return null;
        int key = s.indexOf("\"phrase\"");
        if (key < 0) return null;
        int open = s.indexOf('"', s.indexOf(':', key) + 1);
        int close = s.lastIndexOf('"');
        if (open < 0 || close <= open) return null;
        String pat = s.substring(open + 1, close);
        if (SieveToken.stripComments(pat).isBlank()) return null;
        return pat;
    }

    private static String[] extractDef(String line) {
        String s = line.trim();
        if (s.isEmpty() || s.startsWith("#") || !s.contains("\"function\"")) return null;
        String name = jsonValue(s, "function");
        String is = jsonValue(s, "is");
        if (name == null || is == null) return null;
        return new String[]{name, is};
    }

    private static final String[] PROBES = {
        "", "a", "the", "hello", "minecraft", "scunthorpe", "raccoon", "analytics", "nigeria",
        "pedo", "pedometer", "pedophile", "cunt", "scunt", "fag", "faggot", "fggaot", "nigger",
        "niger", "snigger", "retard", "retarded", "trading", "tarding", "kill", "yourself",
        "killyourself", "cut", "wrists", "cutyourwrists", "me", "essentials:me", "minecraft:me",
        "say", "msg", "w", "namespace:w", "x:say", "child", "porn", "childporn", "child porn",
        "dyke", "vandyke", "van dyke", "shemale", "wop", "doo wop", "groom", "groomer",
        "groom my son", "groomsmen",
    };

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: OracleGen <config.jsonl> <out.tsv>");
            System.exit(2);
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) lines.add(l);
        }

        SieveToken.resetClasses();
        for (String line : lines) {
            String[] def = extractDef(line);
            if (def != null) {
                String name = (def[0].startsWith("&") || def[0].startsWith("^"))
                        ? def[0].substring(1) : def[0];
                SieveToken.registerClass(name, Arrays.asList(def[1].split("\\|", -1)));
            }
        }
        List<String> patterns = new ArrayList<>();
        List<SieveToken> tokens = new ArrayList<>();
        for (String line : lines) {
            String pat = extractPattern(line);
            if (pat == null) continue;
            patterns.add(pat);
            tokens.add(SieveToken.compile(pat));
        }

        try (PrintWriter out = new PrintWriter(args[1], StandardCharsets.UTF_8)) {
            for (int i = 0; i < tokens.size(); i++) {
                SieveToken t = tokens.get(i);
                String pat = patterns.get(i);

                List<String> cands = new ArrayList<>(Arrays.asList(PROBES));
                for (String w : t.concreteWords()) {
                    cands.add(w);
                    cands.add(w + "s");
                    cands.add("z" + w);
                }
                String tag = t.isSelfHarm() ? "sh" : t.isAbuse() ? "ea" : t.isProfanity() ? "pf" : "-";
                String gated = t.isGated() ? "gated" : "-";
                for (String c : cands) {
                    boolean m = t.matches(c);
                    out.println(i + "\t" + pat + "\t" + c + "\t" + (m ? "MATCH" : "nomatch")
                            + "\t" + tag + "\t" + gated);
                }
            }
        }
        System.err.println("oracle: " + tokens.size() + " patterns from " + args[0]);
    }
}
