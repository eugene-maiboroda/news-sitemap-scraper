package com.article.application;

import com.article.application.dto.ExportData;
import com.article.domain.*;
import com.article.bootstrap.CollectorProps;
import com.article.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectorService {

    private final ArticlePersistencePort articlePersistencePort;
    private final VideoPersistencePort videoPersistencePort;
    private final ImagePersistencePort imagePersistencePort;
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
                articlePersistencePort.insertLinks(links);
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

        List<ArticleLink> batch;

//        batch.add(ArticleLink.builder()
//                .articleDate(LocalDate.EPOCH)
//                .articleUrl("https://nypost.com/2024/02/11/lifestyle/dear-abby-im-gay-but-women-keep-coming-on-to-me/")
//                .build());
        while (!(batch = articlePersistencePort.findNewBatch(batchSize)).isEmpty()) {
            parallelExecutor.execute(batch, this::parseSingle);
        }
        log.info("No more links to parse");
    }

    private void parseSingle(ArticleLink link) {
        long start = System.currentTimeMillis();
        switch (articlePagePort.parse(link)) {
            case ParseResult.Success success -> {
                articlePersistencePort.saveArticleAndMarkParsed(success.article(), link.articleUrl());
                parsedMediaService.saveImages(success.images());
                parsedMediaService.saveVideos(success.videos());
                log.info("[PARSED] {} ({} ms)", link.articleUrl(), System.currentTimeMillis() - start);
            }
            case ParseResult.Failure f -> {
                articlePersistencePort.markError(link.articleUrl(), f.error().reason());
                log.warn("[ERROR] {} | {} ({} ms)", link.articleUrl(), f.error().reason(), System.currentTimeMillis() - start);            }
        }
    }

    private void downloadImages() {
        if (!props.steps().downloadImages()) {
            return;
        }
        log.info("Step: DOWNLOAD IMAGES");
        List<ArticleImageRef> imageRefs = articlePersistencePort.findImageRefsForDelivery(props.imageBatchSize());
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
        List<Article> articles = articlePersistencePort.findAllArticles();
        List<Image> images = imagePersistencePort.findAllImages();
        List<Video> videos = videoPersistencePort.findAllVideos();

        ExportData data = new ExportData(articles, images, videos);
        exportPort.export(data);
        log.info("Export finished");
    }
}
