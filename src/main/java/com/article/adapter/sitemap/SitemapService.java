package com.article.adapter.sitemap;

import com.article.adapter.http.HttpContentLoader;
import com.article.domain.ArticleLink;
import com.article.port.SitemapPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SitemapService implements SitemapPort {

    private final HttpContentLoader contentLoader;
    private final NyPostYearSitemapParser yearSitemapParser;
    private final NyPostDaySitemapParser daySitemapParser;

    @Override
    public List<ArticleLink> discoverLinks(String sitemapUrl) {
        String xml = contentLoader.loadText(sitemapUrl);
        List<String> dayUrls = yearSitemapParser.extractDayUrls(xml);
        log.info("Found {} day sitemaps in {}", dayUrls.size(), sitemapUrl);

        List<ArticleLink> allLinks = new ArrayList<>();
        for (String dayUrl : dayUrls) {
            try {
                Thread.sleep(150);
                String dayXml = contentLoader.loadText(dayUrl);
                allLinks.addAll(daySitemapParser.parseLinks(dayXml));
            } catch (Exception e) {
                log.warn("Failed to fetch day sitemap: {}", dayUrl, e);
            }
        }

        log.info("Discovered {} article links from {}", allLinks.size(), sitemapUrl);
        return allLinks;
    }
}
