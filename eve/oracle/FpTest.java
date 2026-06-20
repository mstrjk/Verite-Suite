import teacommontea.veritedoux.SieveToken;
import java.io.*;
import java.util.*;

public class FpTest {
    public static void main(String[] a) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(a[0]))) {
            String l;
            while ((l = r.readLine()) != null) lines.add(l);
        }
        SieveToken.resetClasses();
        for (String line : lines) {
            String s = line.trim();
            if (s.startsWith("#") || !s.contains("\"function\"")) continue;
            int k = s.indexOf("\"function\"");
            int o = s.indexOf('"', s.indexOf(':', k) + 1);
            int c = s.indexOf('"', o + 1);
            String nm = s.substring(o + 1, c);
            int k2 = s.indexOf("\"is\"");
            int o2 = s.indexOf('"', s.indexOf(':', k2) + 1);
            int c2 = s.indexOf('"', o2 + 1);
            String is = s.substring(o2 + 1, c2);
            if (nm.startsWith("&") || nm.startsWith("^")) nm = nm.substring(1);
            SieveToken.registerClass(nm, Arrays.asList(is.split("\\|", -1)));
        }
        List<SieveToken> toks = new ArrayList<>();
        for (String line : lines) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#") || s.contains("\"function\"")) continue;
            int k = s.indexOf("\"phrase\"");
            if (k < 0) continue;
            int o = s.indexOf('"', s.indexOf(':', k) + 1);
            int c = s.lastIndexOf('"');
            String pat = s.substring(o + 1, c);
            if (SieveToken.stripComments(pat).isBlank()) continue;
            toks.add(SieveToken.compile(pat));
        }
        String[] innocent = {
            "scunthorpe", "raccoon", "analytics", "nigeria", "cocktail", "class", "glass", "assess",
            "passing", "kissing", "missing", "bassist", "cooking", "booking", "looking",
            "running", "singer", "finger", "ringer", "bringer", "slinger", "dinger", "banger", "hanger",
            "winged", "ringed", "singing", "ringing", "bringing", "stringing", "shoring", "boring",
            "snoring", "coring", "scoring", "flooring", "mooring", "pouring", "touring", "souring",
            "wiring", "firing", "hiring", "tiring", "retiring", "aspiring", "desiring", "requiring",
            "bagged", "tagged", "gagged", "nagged", "ragged", "sagged", "wagged", "dragged", "flagged",
            "snagged", "humming", "summing", "drumming", "strumming", "plumbing", "numbering",
            "cocked", "blocked", "clocked", "docked", "flocked", "knocked", "locked", "mocked",
            "rocked", "shocked", "stocked", "analyst", "analysts", "title", "titles", "titter",
            "tittering", "fitter", "bitter", "sitter", "litter", "glitter", "critter", "cocker",
            "blocker", "clocker", "docker", "locker", "mocker", "rocker", "shocker", "stocker",
            "cracker", "hiked", "biked", "liked", "spiked", "cooped", "looped", "scooped", "title",
            "facer", "pacer", "racer", "tracer", "spacer", "placer", "fishing", "wishing", "dishing"
        };
        int fp = 0;
        for (String w : innocent) {
            for (SieveToken t : toks) {
                if (t.matches(w, w)) {
                    System.out.println("FALSE POSITIVE: \"" + w + "\"");
                    fp++;
                    break;
                }
            }
        }
        System.out.println("\n" + innocent.length + " innocent words tested, " + fp + " false positives");
    }
}
