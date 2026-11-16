import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
class Evidence {
 static String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","")+"\"";}
 static String arr(Collection<String> s){return "["+String.join(",",s.stream().map(Evidence::q).toList())+"]";}
 public static void main(String[] args)throws Exception{
  var files=Files.list(Path.of("work/nra-audit")).filter(p->p.toString().endsWith(".html") || p.getFileName().toString().equals("nra-news.txt")).sorted().toList();
  var rows=new ArrayList<String>();
  for(var p:files){
   var d=Jsoup.parse(p.toFile(),"UTF-8","https://www.americanrifleman.org/");var kv=new ArrayList<String>();
   kv.add("\"file\":"+q(p.getFileName().toString()));kv.add("\"title\":"+q(d.title()));
   for(var sel:List.of("h1",".article-attribution .tag a",".appears_in a",".topic-tags a",".paging-copy","meta[name=publicationdate]","meta[property=article:published_time]","link[rel=canonical]",".latest-stories-list h3 a",".article-rte img",".video-responsive iframe","main video","main source","main iframe","main a[href*=youtube]","script[type=application/ld+json]")){
    kv.add(q(sel)+":"+arr(d.select(sel).stream().map(e->e.tagName().equals("meta")?e.attr("content"):e.tagName().equals("script")?e.data():e.tagName().equals("img")?e.attr("src")+" | caption="+e.attr("data-caption"):e.tagName().equals("iframe")||e.tagName().equals("source")?e.attr("src"):e.text()+" | "+e.attr("href")).toList()));
   }
   for(var sel:List.of(".article-header h1",".entry-content","article","figure.is-provider-youtube iframe",".article-rte",".article-content",".article-rte img",".article-rte img[data-udi]",".article-rte figure img"))kv.add(q("count:"+sel)+":"+d.select(sel).size());
   rows.add("{"+String.join(",",kv)+"}");
  }Files.writeString(Path.of("work/nra-audit/evidence.json"),"["+String.join(",\n",rows)+"]");
 }
}
