package com.article.application;


import com.article.domain.article.ArticleLink;
import com.article.port.out.SitemapDiscoveryPort;
import com.article.port.out.persistence.ArticlePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscoverLinksUseCase {

    private final ArticlePersistencePort articlePersistencePort;
    private final SitemapDiscoveryPort sitemapDiscoveryPort;
    private final PipelineRunSettings runSettings;

    public void discoverLinks(List<String> sitemapUrls, int batchSize) {
        log.info("Step: DISCOVER LINKS");

        if (sitemapUrls == null || sitemapUrls.isEmpty()) {
            log.warn("No sitemap URLs configured. Add urls to collector.sitemap-urls in application.properties");
            return;
        }

        log.info("NYPost Article Collector started | sitemaps={}", sitemapUrls.size());

        for (String sitemapUrl : sitemapUrls) {
            if (runSettings.isStopRequested()) {
                break;
            }
            List<ArticleLink> links;
            try {
                links = sitemapDiscoveryPort.discoverLinks(sitemapUrl);
            } catch (Exception e) {
                log.error("Failed to discover links from {}", sitemapUrl, e);
                continue;
            }

            insertInBatches(links, batchSize);
            log.info("Discovered {} links from {}", links.size(), sitemapUrl);
        }
    }

    private void insertInBatches(List<ArticleLink> links, int batchSize) {
        for (int from = 0; from < links.size(); from += batchSize) {
            int to = Math.min(from + batchSize, links.size());
            articlePersistencePort.insertLinks(links.subList(from, to));
        }
    }
}
