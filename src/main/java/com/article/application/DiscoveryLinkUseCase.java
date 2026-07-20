package com.article.application;


import com.article.bootstrap.properties.CollectorProps;
import com.article.domain.ArticleLink;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.SitemapPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscoveryLinkUseCase {
    private final CollectorProps props;
    private final ArticlePersistencePort articlePersistencePort;
    private final SitemapPort sitemapPort;

    public void discoverLinks() {
        log.info("Step: DISCOVER LINKS");
        List<String> sitemapUrls = props.sitemapUrls();

        if (sitemapUrls == null || sitemapUrls.isEmpty()) {
            log.warn("No sitemap URLs configured. Add urls to collector.sitemap-urls in application.properties");
            return;
        }

        log.info("NYPost Article Collector started | sitemaps={}", sitemapUrls.size());

        for (String sitemapUrl : sitemapUrls) {
            try {
                List<ArticleLink> links = sitemapPort.discoverLinks(sitemapUrl);
                articlePersistencePort.insertLinks(links);
                log.info("Discovered {} links from {}", links.size(), sitemapUrl);
            } catch (Exception e) {
                log.error("Failed to discover links from {}", sitemapUrl, e);
            }
        }
    }
}
