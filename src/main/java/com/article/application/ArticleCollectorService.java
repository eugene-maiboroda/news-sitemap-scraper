package com.article.application;

import com.article.domain.Article;
import com.article.domain.ArticleImageRef;
import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;
import com.article.domain.Video;
import com.article.bootstrap.CollectorProps;
import com.article.port.ArticlePagePort;
import com.article.port.ArticleRepositoryPort;
import com.article.port.ExportPort;
import com.article.port.ImageDownloadPort;
import com.article.port.SitemapPort;
import com.article.port.VideoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final VideoRepositoryPort videoRepositoryPort;
    private final CollectorProps properties;
    private final ParallelExecutor parallelExecutor;

    public void run(List<String> sitemapUrls) {
        log.info("NYPost Article Collector started | sitemaps={}", sitemapUrls.size());

        if (properties.steps().discoverLinks()) {
            log.info("Step: DISCOVER LINKS");
            for (String sitemapUrl : sitemapUrls) {
                try {
                    List<ArticleLink> links = sitemapPort.discoverLinks(sitemapUrl);
                    repositoryPort.insertLinks(links);
                    log.info("Discovered {} links from {}", links.size(), sitemapUrl);
                } catch (Exception e) {
                    log.error("Failed to discover links from {}", sitemapUrl, e);
                }
            }
        }

        if (properties.steps().parsePages()) {
            log.info("Step: PARSE PAGES");
            parsePages();
        }

        if (properties.steps().downloadImages()) {
            log.info("Step: DOWNLOAD IMAGES");
            List<ArticleImageRef> imageRefs = repositoryPort.findImageRefsForDelivery(properties.deliveryLimit());
            log.info("Downloading {} images", imageRefs.size());
            parallelExecutor.execute(imageRefs, imageDownloadPort::download);
            log.info("Download finished");
        }

        if (properties.steps().exportJson()) {
            log.info("Step: EXPORT JSON");
            List<Article> articles = repositoryPort.findAllArticles();
            exportPort.export(articles);
            log.info("Export finished");
        }

        log.info("NYPost Article Collector finished");
    }

    private void parsePages() {
        int batchSize = properties.parseBatchSize();
        int threads = properties.parseThreads();
        log.info("Parsing pages | batchSize={} threads={}", batchSize, threads);

        List<ArticleLink> batch = new ArrayList<>();

        batch.add(ArticleLink.builder().articleUrl("https://nypost.com/2026/06/04/business/techno-optimism-is-on-full-display-at-ny-tech-week/")
                .build());
//        while (!(batch = repositoryPort.findNewBatch(batchSize)).isEmpty()) {
//        while (!(batch = repositoryPort.findNewBatch(batchSize)).isEmpty()) {
//            parallelExecutor.execute(batch, this::parseSingle);
//        }
        parseSingle(batch.get(0));
        log.info("No more links to parse");
    }

    private void parseSingle(ArticleLink link) {
        switch (articlePagePort.parse(link)) {
            case ParseResult.Ok ok -> {
                repositoryPort.saveArticleAndMarkParsed(ok.article(), link.articleUrl());
                saveVideos(ok.videos());
                log.info("[PARSED] {}", link.articleUrl());
            }
            case ParseResult.Failure f -> {
                repositoryPort.markError(link.articleUrl(), f.error().reason());
                log.warn("[ERROR] {} | {}", link.articleUrl(), f.error().reason());
            }
        }
    }

    private void saveVideos(List<Video> videos) {
        for (Video video : videos) {
            boolean exists = switch (video.platform()) {
                case JW_TOP -> videoRepositoryPort.existsByArticleIdAndJwMediaId(video.articleId(), video.jwMediaId());
                case CONNATIX_BOTTOM -> videoRepositoryPort.existsByArticleIdAndConnatixMmid(video.articleId(), video.connatixMmid());
            };
            if (exists) {
                log.debug("[VIDEO][SKIP] duplicate platform={} articleId={}", video.platform(), video.articleId());
                continue;
            }
            videoRepositoryPort.save(video);
            log.info("[VIDEO][SAVED] platform={} articleId={}", video.platform(), video.articleId());
        }
    }
}
