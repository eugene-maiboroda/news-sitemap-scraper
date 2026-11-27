package com.article.adapter.sitemap;

import com.article.adapter.http.HttpContentLoader;
import com.article.application.PipelineRunSettings;
import com.article.domain.article.ArticleLink;
import com.article.port.out.SitemapDiscoveryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SitemapService implements SitemapDiscoveryPort {

    private final HttpContentLoader contentLoader;
    private final NyPostYearSitemapParser yearSitemapParser;
    private final NyPostDaySitemapParser daySitemapParser;
    private final PipelineRunSettings runSettings;

    @Override
    public List<ArticleLink> discoverLinks(String sitemapUrl) {
        if (runSettings.isStopRequested()) {
            return List.of();
        }

        String xml = contentLoader.loadText(sitemapUrl);
        List<String> dayUrls = yearSitemapParser.extractDayUrls(xml);
        log.info("Found {} day sitemaps in {}", dayUrls.size(), sitemapUrl);

        List<ArticleLink> allLinks = new ArrayList<>();
        for (String dayUrl : dayUrls) {
            if (runSettings.isStopRequested()) {
                break;
            }
            try {
                Thread.sleep(runSettings.current().collector().sitemapDelayMs());
                if (runSettings.isStopRequested()) {
                    break;
                }
                String dayXml = contentLoader.loadText(dayUrl);
                allLinks.addAll(daySitemapParser.parseLinks(dayXml));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Sitemap discovery interrupted");
                break;
            } catch (Exception e) {
                log.warn("Failed to fetch day sitemap: {}", dayUrl, e);
            }
        }

        log.info("Discovered {} article links from {}", allLinks.size(), sitemapUrl);
        return allLinks;
    }
}
