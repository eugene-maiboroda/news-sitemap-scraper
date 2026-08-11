package com.article.application;

import com.article.domain.*;
import com.article.infrastructure.CollectorProps;
import com.article.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectorService {

    private final ArticleRepositoryPort repositoryPort;
    private final SitemapPort sitemapPort;
    private final ArticlePagePort articlePagePort;
    private final ExportPort exportPort;
    private final ImageDownloadPort imageDownloadPort;
    private final CollectorProps properties;
    private final ParallelExecutor parallelExecutor;

    public void run(List<TeamConfig> teams) {
        log.info("News sitemap scraper started | teams={}", teams.size());

        if (properties.steps().collectLinks()) {
            log.info("Step: COLLECT LINKS");
            for (TeamConfig team : teams) {
                try {
                    collectLinks(team);
                } catch (Exception e) {
                    log.error("Failed to collect links for team {}", team.teamName(), e);
                }
            }
        }

        if (properties.steps().parsePages()) {
            log.info("Step: PARSE PAGES");
            parsePages();
        }

        if (properties.steps().downloadImages()) {
            int threads = properties.imageThreads();
            log.info("Step: DOWNLOAD IMAGES");
            List<ArticleImageRef> imageRefs = repositoryPort.findImageRefsForDelivery(properties.deliveryLimit());
            log.info("Downloading {} images", imageRefs.size());
            parallelExecutor.execute(imageRefs, imageDownloadPort::download, threads);
            log.info("Download finished");
        }

        if (properties.steps().exportJson()) {
            log.info("Step: EXPORT JSON");
            List<Article> articles = repositoryPort.findAllArticles();
            exportPort.export(articles);
            log.info("Export finished");
        }

        log.info("News sitemap scraper finished");
    }

    private void collectLinks(TeamConfig team) {
        log.info("Collecting links | team={} range={}..{}", team.teamName(), team.startDate(), team.endDate());

        List<String> quarterUrls = sitemapPort.loadQuarterUrls(team.teamSitemapUrl());

        log.info("Quarter sitemaps found: {}", quarterUrls.size());

        List<ArticleLink> allLinks = new ArrayList<>();
        for (String quarterUrl : quarterUrls) {
            allLinks.addAll(sitemapPort.loadArticleLinks(quarterUrl, team.teamName()));
        }

        List<ArticleLink> filtered = filterByDateRange(allLinks, team.startDate(), team.endDate());
        log.info("Links collected={} filtered={}", allLinks.size(), filtered.size());

        if (!filtered.isEmpty()) {
            repositoryPort.insertLinks(filtered);
        }
    }

    private void parsePages() {
        int batchSize = properties.parseBatchSize();
        int threads = properties.parseThreads();
        log.info("Parsing pages | batchSize={} threads={}", batchSize, threads);

        while (true) {
            List<ArticleLink> batch = repositoryPort.findUnparsedBatch(batchSize);
            if (batch.isEmpty()) {
                log.info("No more links to parse");
                break;
            }

            parallelExecutor.execute(batch, this::parseSingle, threads);

        }
    }

    private void parseSingle(ArticleLink link) {
        try {
            Article article = articlePagePort.parse(link);
            repositoryPort.saveArticleAndMarkParsed(article, link.articleUrl());
            log.info("[PARSED] {} | {}", link.teamName(), link.articleUrl());
        } catch (Exception e) {
            log.error("[FAILED] {} | {}", link.teamName(), link.articleUrl(), e);
        }
    }

    private List<ArticleLink> filterByDateRange(List<ArticleLink> links, LocalDate start, LocalDate end) {
        return links.stream()
                .filter(l -> !l.articleDate().isBefore(start) && !l.articleDate().isAfter(end))
                .toList();
    }
}
