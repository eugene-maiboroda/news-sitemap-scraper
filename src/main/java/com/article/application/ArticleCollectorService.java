package com.article.application;

import com.article.domain.Article;
import com.article.domain.ArticleImageRef;
import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;
import com.article.bootstrap.CollectorProps;
import com.article.port.ArticlePagePort;
import com.article.port.ArticleRepositoryPort;
import com.article.port.ExportPort;
import com.article.port.ImageDownloadPort;
import com.article.port.SitemapPort;
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

    private final ArticleRepositoryPort articleRepositoryPort;
    private final SitemapPort sitemapPort;
    private final ArticlePagePort articlePagePort;
    private final ExportPort exportPort;
    private final ImageDownloadPort imageDownloadPort;
    private final CollectorProps props;
    private final ParallelExecutor parallelExecutor;
    private final ParsedMediaService parsedMediaService;

    public void run() {
        discoverLinks();
        parsePages();
        downloadImages();
//        downloadVideo();
        exportJson();

        log.info("NYPost Article Collector finished");
    }

    private void discoverLinks() {
        if (!props.steps().discoverLinks()) {
            return;
        }
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
                articleRepositoryPort.insertLinks(links);
                log.info("Discovered {} links from {}", links.size(), sitemapUrl);
            } catch (Exception e) {
                log.error("Failed to discover links from {}", sitemapUrl, e);
            }
        }
    }

    private void parsePages() {
        if (!props.steps().parsePages()) {
            return;
        }
        log.info("Step: PARSE PAGES");

        int batchSize = props.parseBatchSize();
        int threads = props.parseThreads();
        log.info("Parsing pages | batchSize={} threads={}", batchSize, threads);

        List<ArticleLink> batch = new ArrayList<>();

        batch.add(ArticleLink.builder()
                .articleDate(LocalDate.EPOCH)
                .articleUrl("https://pagesix.com/2016/06/05/kardashians-ellen-degeneres-among-stars-affected-by-la-fire/")
                .build());
//        while (!(batch = articleRepositoryPort.findNewBatch(batchSize)).isEmpty()) {
            parallelExecutor.execute(batch, this::parseSingle);
//        }
        parseSingle(batch.getFirst());
        log.info("No more links to parse");
    }

    private void parseSingle(ArticleLink link) {
        long start = System.currentTimeMillis();
        switch (articlePagePort.parse(link)) {
            case ParseResult.Success success -> {
                articleRepositoryPort.saveArticleAndMarkParsed(success.article(), link.articleUrl());
                parsedMediaService.saveImages(success.images());
                parsedMediaService.saveVideos(success.videos());
                log.info("[PARSED] {} ({} ms)", link.articleUrl(), System.currentTimeMillis() - start);
            }
            case ParseResult.Failure f -> {
                articleRepositoryPort.markError(link.articleUrl(), f.error().reason());
                log.warn("[ERROR] {} | {} ({} ms)", link.articleUrl(), f.error().reason(), System.currentTimeMillis() - start);            }
        }
    }

    private void downloadImages() {
        if (!props.steps().downloadImages()) {
            return;
        }
        log.info("Step: DOWNLOAD IMAGES");
        List<ArticleImageRef> imageRefs = articleRepositoryPort.findImageRefsForDelivery(props.imageBatchSize());
        log.info("Downloading {} images", imageRefs.size());
        parallelExecutor.execute(imageRefs, imageDownloadPort::download);
        log.info("Download finished");
    }

    private void downloadVideo() {

    }

    private void exportJson() {
        if (!props.steps().exportJson()) {
            return;
        }
        log.info("Step: EXPORT JSON");
        List<Article> articles = articleRepositoryPort.findAllArticles();
        exportPort.export(articles);
        log.info("Export finished");
    }
}
