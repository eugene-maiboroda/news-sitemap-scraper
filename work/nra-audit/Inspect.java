import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.io.File;
class Inspect {
 public static void main(String[] args) throws Exception {
  var d=Jsoup.parse(new File(args[0]),"UTF-8", "https://www.americanrifleman.org/");
  for(int i=1;i<args.length;i++) {
   System.out.println("SELECTOR: "+args[i]+" COUNT: "+d.select(args[i]).size());
   for(var e:d.select(args[i]))System.out.println(e.outerHtml());
  }
 }
}
