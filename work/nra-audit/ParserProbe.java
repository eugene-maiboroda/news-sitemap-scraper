import java.nio.file.*;
import org.jsoup.Jsoup;
import com.article.adapter.video.YoutubeVideoExtractor;
import com.article.adapter.shared.ParseUtils;
class ParserProbe {
 public static void main(String[] args)throws Exception{
  for(var n:args){var d=Jsoup.parse(Path.of("work/nra-audit",n).toFile(),"UTF-8","https://www.americanrifleman.org/");
   System.out.printf("%s\tNY_title=%d\tNY_body=%d\tYT_extracted=%s\tbody_imgs=%d\taccepted_body_imgs=%d%n",n,d.select(".article-header h1").size(),d.select(".entry-content").size(),new YoutubeVideoExtractor().extract(d,"audit","https://example.org/").isPresent(),d.select(".article-rte img").size(),d.select(".article-rte img").stream().filter(ParseUtils::isLikelyContentImage).count());
  }
 }
}
