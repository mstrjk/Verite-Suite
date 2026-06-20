import teacommontea.eve.Eve;
import java.nio.file.*;
public class LinuxLive {
  public static void main(String[] a) throws Exception {
    if(!Eve.nativeAvailable()){Eve.nativeError().printStackTrace();System.exit(1);}
    try(Eve eve=Eve.parse(Files.readString(Path.of(a[0])))){
      System.out.println("linux parse OK: "+eve.ruleCount()+" rules");
      for(String[] c:new String[][]{{"faggot","BLOCK"},{"kms","SELF_HARM"},{"scunthorpe","clean"},{"nigger","BLOCK"},{"masturbating","BLOCK?"}}){
        String w="clean"; for(String word:c[0].split(" ")) for(var m:eve.scan(word)){ if(m.flag("sh",false))w="SELF_HARM"; else if(w.equals("clean"))w="BLOCK"; }
        System.out.printf("  %-14s -> %s%n", c[0], w);
      }
    }
  }
}
