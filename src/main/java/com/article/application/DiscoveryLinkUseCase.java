package com.article.application;

import com.article.bootstrap.properties.CollectorProps;
import com.article.domain.ArticleLink;
import com.article.domain.DiscoveredArticle;
import com.article.port.MenuDiscoveryPort.Section;
import com.article.port.MenuDiscoveryPort.SectionResult;
import com.article.port.MenuDiscoveryPort;
import com.article.port.SitemapPort;
import com.article.port.persistence.ArticlePersistencePort;
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
    private final MenuDiscoveryPort menuDiscovery;

    public void discoverLinks() {
        log.info("Step: DISCOVER LINKS");
        if (props.discoverySource() == CollectorProps.DiscoverySource.AMERICAN_HUNTER) {
            discoverMenuLinks();
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

    private void discoverMenuLinks() {
        int failedSections = 0;
        for (Section section : menuDiscovery.sections()) {
            if (!section.collectCards()) {
                log.info("Reference page: {}", section.path());
                continue;
            }

            try {
                SectionResult result = menuDiscovery.discover(section);
                saveSectionCards(section, result);
                if (result.error() != null) {
                    failedSections++;
                    log.warn("Menu section incomplete: {}", result.error());
                }
            } catch (RuntimeException e) {
                failedSections++;
                log.error("Failed to save menu section {}", section.path(), e);
            }
        }
        log.info("Menu discovery finished with {} incomplete or failed sections", failedSections);
    }

    private void saveSectionCards(Section section, SectionResult result) {
        Map<String, List<List<String>>> menuPaths = Map.of(section.path(), section.hierarchies());
        List<DiscoveredArticle> articles = result.cards().stream()
                .map(card -> new DiscoveredArticle(card.articleUrl(), card.title(), card.description(), menuPaths))
                .toList();

        if (!articles.isEmpty()) {
            articlePersistencePort.saveDiscoveredArticles(articles);
        }
        log.info("Section {}: {} unique URLs, {} article-menu paths saved",
                section.path(), articles.size(), (long) articles.size() * section.hierarchies().size());
    }
}
