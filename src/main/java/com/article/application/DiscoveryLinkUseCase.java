package com.article.application;

import com.article.adapter.discovery.AmericanHunterDiscoveryAdapter;
import com.article.adapter.discovery.AmericanHunterDiscoveryAdapter.Section;
import com.article.adapter.discovery.AmericanHunterDiscoveryAdapter.SectionResult;
import com.article.bootstrap.properties.CollectorProps;
import com.article.domain.ArticleLink;
import com.article.domain.DiscoveredArticle;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.SitemapPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscoveryLinkUseCase {
    private final CollectorProps props;
    private final ArticlePersistencePort articlePersistencePort;
    private final SitemapPort sitemapPort;
    private final AmericanHunterDiscoveryAdapter hunterDiscovery;

    public void discoverLinks() {
        log.info("Step: DISCOVER LINKS");
        if (props.discoverySource() == CollectorProps.DiscoverySource.AMERICAN_HUNTER) {
            discoverHunterLinks();
            return;
        }
        discoverSitemapLinks();
    }

    private void discoverSitemapLinks() {
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

    private void discoverHunterLinks() {
        int failedSections = 0;
        for (Section section : hunterDiscovery.sections()) {
            if (!section.collectCards()) {
                log.info("Hunter reference page: {}", section.path());
                continue;
            }

            try {
                SectionResult result = hunterDiscovery.discover(section);
                Map<String, List<List<String>>> menuPaths = Map.of(section.path(), section.hierarchies());
                List<DiscoveredArticle> articles = result.cards().stream()
                        .map(card -> new DiscoveredArticle(card.articleUrl(), card.title(), card.description(), menuPaths))
                        .toList();
                if (!articles.isEmpty()) {
                    articlePersistencePort.saveDiscoveredArticles(articles);
                }
                log.info("Hunter {}: {} unique URLs, {} article-menu paths saved",
                        section.path(), articles.size(), (long) articles.size() * section.hierarchies().size());
                if (result.error() != null) {
                    failedSections++;
                    log.warn("Hunter section incomplete: {}", result.error());
                }
            } catch (RuntimeException e) {
                failedSections++;
                log.error("Failed to save Hunter section {}", section.path(), e);
            }
        }
        log.info("Hunter discovery finished with {} incomplete or failed sections", failedSections);
    }
}
