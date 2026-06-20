import teacommontea.eve.Eve;
import java.nio.file.*;
public class ScanTest {
  public static void main(String[] a) throws Exception {
    if (!Eve.nativeAvailable()) { Eve.nativeError().printStackTrace(); System.exit(1); }
    try (Eve eve = Eve.parse(Files.readString(Path.of(a[0])))) {
      for (String word : new String[]{"faggot","kms","scunthorpe"}) {
        var ms = eve.scan(word);
        System.out.println(word + " -> " + ms.size() + " match(es)");
        for (var m : ms)
          System.out.printf("   rule#%d %s [%s] flags=%s%n", m.rule(), m.name(), m.realm(), m.flags());
      }
    }
  }
}
