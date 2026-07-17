package com.article.application;

import com.article.adapter.article.MainCategoryResolver;
import com.article.adapter.article.TopicsTagsExtractor;
import com.article.adapter.export.dto.LinkDto;
import com.article.application.dto.ExportData;
import com.article.application.dto.TopicExportContext;
import com.article.domain.*;
import com.article.domain.status.DownloadStatus;
import com.article.bootstrap.CollectorProps;
import com.article.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectorService {

    private final ArticlePersistencePort articlePersistencePort;
    private final ArticleHtmlPersistencePort articleHtmlPersistencePort;
    private final VideoPersistencePort videoPersistencePort;
    private final ImagePersistencePort imagePersistencePort;
    private final SitemapPort sitemapPort;
    private final ArticlePagePort articlePagePort;
    private final ExportPort exportPort;
    private final ImageDownloadPort imageDownloadPort;
    private final VideoDownloadPort videoDownloadPort;
    private final CollectorProps props;
    private final ParallelExecutor parallelExecutor;
    private final ParsedMediaService parsedMediaService;
    private final TopicsTagsExtractor topicsTagsExtractor;
    private final ArticleTopicsPersistencePort articleTopicsPersistencePort;
    private final MainCategoryResolver mainCategoryResolver;
    private final AuthorUseCase authorUseCase;

    public void run() {
        discoverLinks();
        parsePages();
        extractTopics();
        downloadImages();
        downloadVideo();
        exportJson();

//        log.info("Author parsed and updated");
//        authorUseCase.parseAuthors();
//        log.info("Authors parsed and updated");

//        authorUseCase.downloadAvatars();
//        log.info("Author avatars downloaded");

//        authorUseCase.exportAuthors();
//        log.info("Authors exported");

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

    private void extractTopics() {
        if (!props.steps().extractTopics()) {
            return;
        }
        log.info("Step: EXTRACT TOPICS");
        int batchSize = props.parseBatchSize();
        int batchNumber = 0;
        int processed = 0;
        AtomicInteger topicOverflowCount = new AtomicInteger();
        List<ArticleRawHtml> batch;
        while (!(batch = articleTopicsPersistencePort.findPendingBatch(batchSize)).isEmpty()) {
            batchNumber++;
            List<ArticleTopicsResult> results =
                    parallelExecutor.map(batch, ref -> resolveTopics(ref, topicOverflowCount));
            if (!results.isEmpty()) {
                articleTopicsPersistencePort.saveTopicsBatch(results);
            }
            processed += batch.size();
            log.info("Processing batch {} | batch size: {} | processed so far: {}",
                    batchNumber, batch.size(), processed);
        }
        log.info("Extract topics finished ({} articles, {} batches)", processed, batchNumber);
        if (topicOverflowCount.get() > 0) {
            log.info("Articles with more than 5 topics: {}", topicOverflowCount.get());
        }
    }

    private ArticleTopicsResult resolveTopics(ArticleRawHtml ref, AtomicInteger topicOverflowCount) {
        List<LinkDto> topics = topicsTagsExtractor.extract(ref.articleId(), ref.rawHtml()).topics();
        if (topics.size() > 5) {
            topicOverflowCount.incrementAndGet();
            log.debug("Article {} has {} topics. Only first 5 topics will be stored.", ref.articleId(), topics.size());
        }
        String topic1Name = topics.isEmpty() ? null : topics.get(0).name();
        String topic1Url = topics.isEmpty() ? null : topics.get(0).url();
        String mainCategory = mainCategoryResolver.resolve(topic1Name, topic1Url);
        return new ArticleTopicsResult(ref.articleId(), topics, mainCategory);
    }

    private void downloadImages() {
        if (!props.steps().downloadImages()) {
            return;
        }
        log.info("Step: DOWNLOAD IMAGES");

        List<String> allCategories = articlePersistencePort.findDistinctCategories();
        List<CategoryPendingCount> pendingCategories = imagePersistencePort.findCategoriesOrderedByPendingCount();
        log.info("Pending categories: {} of {} total", pendingCategories.size(), allCategories.size());

        int total = pendingCategories.size();
        int index = 0;
        for (CategoryPendingCount entry : pendingCategories) {
            index++;
            log.info("Category {}/{} — {} (pending: {})", index, total, entry.category(), entry.pendingCount());
            downloadBatches(entry.category(),
                    () -> imagePersistencePort.findPendingImagesForCategory(entry.category(), props.imageBatchSize()));
        }

        log.info("Uncategorized — starting");
        downloadBatches("uncategorized",
                () -> imagePersistencePort.findPendingImagesWithoutCategory(props.imageBatchSize()));

        log.info("Download finished");
    }

    private void downloadBatches(String label, Supplier<List<ArticleImageRef>> fetch) {
        int batches = 0;
        int downloaded = 0;
        List<ArticleImageRef> batch;
        while (!(batch = fetch.get()).isEmpty()) {
            parallelExecutor.execute(batch, this::downloadOne);
            batches++;
            downloaded += batch.size();
        }
        log.info("Category {} finished ({} images, {} batches)", label, downloaded, batches);
    }

    private void downloadOne(ArticleImageRef ref) {
        DownloadResult result = imageDownloadPort.download(ref);
        if (result.status() == DownloadStatus.DOWNLOADED) {
            imagePersistencePort.markDownloaded(ref.id(), result.localFilePath(), result.fileSize());
        } else {
            imagePersistencePort.markFailed(ref.id());
        }
    }

    private void downloadVideo() {
        if (!props.steps().downloadVideos()) {
            return;
        }
        log.info("Step: DOWNLOAD VIDEOS");

        List<String> allCategories = articlePersistencePort.findDistinctCategories();
        List<CategoryPendingCount> pendingCategories = videoPersistencePort.findCategoriesOrderedByPendingCount();
        log.info("Pending categories: {} of {} total", pendingCategories.size(), allCategories.size());

        int total = pendingCategories.size();
        int index = 0;
        for (CategoryPendingCount entry : pendingCategories) {
            index++;
            log.info("Category {}/{} — {} (pending: {})", index, total, entry.category(), entry.pendingCount());
            downloadVideoBatches(entry.category(),
                    () -> videoPersistencePort.findPendingVideosForCategory(entry.category(), props.videoBatchSize()));
        }

        log.info("Uncategorized video — starting");
        downloadVideoBatches("uncategorized",
                () -> videoPersistencePort.findPendingVideosWithoutCategory(props.videoBatchSize()));

        log.info("Download video finished");
    }

    private void downloadVideoBatches(String label, Supplier<List<ArticleVideoRef>> fetch) {
        int batches = 0;
        int downloaded = 0;
        List<ArticleVideoRef> batch;
        while (!(batch = fetch.get()).isEmpty()) {
            parallelExecutor.execute(batch, this::downloadVideoOne);
            batches++;
            downloaded += batch.size();
        }
        log.info("Category {} finished ({} videos, {} batches)", label, downloaded, batches);
    }

    private void downloadVideoOne(ArticleVideoRef ref) {
        DownloadResult result = videoDownloadPort.download(ref);
        if (result.status() == DownloadStatus.DOWNLOADED) {
            videoPersistencePort.markDownloaded(ref.id(), result.localFilePath(), result.fileSize());
        } else {
            videoPersistencePort.markFailed(ref.id());
        }
    }

    private void exportJson() {
        if (!props.steps().exportJson()) {
            return;
        }
        String mode = props.export().mode();
        if ("topics".equals(mode)) {
            exportJsonByTopics();
        } else if ("tags".equals(mode)) {
            exportJsonByTags();
        } else {
            exportJsonByCategory();
        }
    }

    private void exportJsonByCategory() {
        log.info("Step: EXPORT JSON");

        int pageSize = props.parseBatchSize();

        List<String> categories = articlePersistencePort.findDistinctCategories();
        log.info("Exporting {} categories", categories.size());
        for (String category : categories) {
            int page = 0;
            List<Article> batch;
            while (!(batch = articlePersistencePort.findArticlesByCategory(category, page, pageSize)).isEmpty()) {
                exportBatch(category, batch);
                page++;
            }
        }

        int page = 0;
        List<Article> batch;
        while (!(batch = articlePersistencePort.findArticlesWithoutCategory(page, pageSize)).isEmpty()) {
            exportBatch(null, batch);
            page++;
        }

        log.info("Export finished");
    }

    private void exportBatch(String category, List<Article> articles) {
        List<String> articleIds = articles.stream().map(Article::id).toList();
        List<Image> images = imagePersistencePort.findImagesByArticleIds(articleIds);
        List<Video> videos = videoPersistencePort.findVideosByArticleIds(articleIds);

        List<ArticleRawHtml> rawHtmls = articleHtmlPersistencePort.findRawHtmlByArticleIds(articleIds);
        List<ArticleLinks> articleLinks = rawHtmls.stream()
                .map(r -> topicsTagsExtractor.extract(r.articleId(), r.rawHtml()))
                .toList();

        exportPort.export(new ExportData(category, articles, images, videos, articleLinks, null));
    }

    // Tags mode ignores category and topic hierarchy entirely — classification happens inside
    // SportsBettingJsonExportAdapter purely from tags, so this scans every article once regardless
    // of category. Uses exportTagsBatch(...) below instead of the shared exportBatch(...) helper
    // ONLY so db/prepare/export phases can be timed separately for diagnostics, without touching
    // exportBatch() itself (which category mode also uses) — a small, deliberate duplication of its
    // ~6-line body, kept isolated to this method so category mode's behavior/logs are untouched.
    private void exportJsonByTags() {
        log.info("Step: EXPORT JSON (tags)");
        int pageSize = props.parseBatchSize();
        int page = 0;
        long totalArticles = 0;
        List<Article> batch;
        while (true) {
            if (page == 0) {
                log.info("[EXPORT][tags] requesting first page | pageSize={}", pageSize);
            }
            long dbStart = System.currentTimeMillis();
            batch = articlePersistencePort.findAllArticlesOrdered(page, pageSize);
            long dbMs = System.currentTimeMillis() - dbStart;
            if (page == 0) {
                log.info("[EXPORT][tags] first page received | articles={} dbMs={}", batch.size(), dbMs);
            }
            if (batch.isEmpty()) {
                break;
            }
            totalArticles += batch.size();
            exportTagsBatch(page, batch, dbMs);
            page++;
        }
        log.info("[EXPORT][tags] scan finished | pages={} totalArticles={}", page, totalArticles);
        log.info("Export (tags) finished");
    }

    private void exportTagsBatch(int page, List<Article> articles, long dbMs) {
        long prepareStart = System.currentTimeMillis();
        List<String> articleIds = articles.stream().map(Article::id).toList();
        List<Image> images = imagePersistencePort.findImagesByArticleIds(articleIds);
        List<Video> videos = videoPersistencePort.findVideosByArticleIds(articleIds);

        List<ArticleRawHtml> rawHtmls = articleHtmlPersistencePort.findRawHtmlByArticleIds(articleIds);
        List<ArticleLinks> articleLinks = rawHtmls.stream()
                .map(r -> topicsTagsExtractor.extract(r.articleId(), r.rawHtml()))
                .toList();
        long prepareMs = System.currentTimeMillis() - prepareStart;

        long exportStart = System.currentTimeMillis();
        exportPort.export(new ExportData(null, articles, images, videos, articleLinks, null));
        long exportMs = System.currentTimeMillis() - exportStart;

        log.info("[EXPORT][tags] page={} articles={} dbMs={} prepareMs={} exportMs={} totalMs={}",
                page, articles.size(), dbMs, prepareMs, exportMs, dbMs + prepareMs + exportMs);
    }

    private void exportJsonByTopics() {
        log.info("Step: EXPORT JSON (topics)");
        List<TopicTarget> targets = articleTopicsPersistencePort.findDistinctTopicTargets();
        log.info("Exporting {} topic groups", targets.size());
        int pageSize = props.parseBatchSize();
        for (TopicTarget target : targets) {
            int page = 0;
            ArticleTopicsPage batch;
            while (!(batch = articleTopicsPersistencePort.findArticlesByTopic(target, page, pageSize)).articles().isEmpty()) {
                exportTopicBatch(target, batch);
                page++;
            }
        }
        log.info("Export (topics) finished");
    }

    private void exportTopicBatch(TopicTarget target, ArticleTopicsPage page) {
        List<Article> articles = page.articles();
        List<String> articleIds = articles.stream().map(Article::id).toList();
        List<Image> images = imagePersistencePort.findImagesByArticleIds(articleIds);
        List<Video> videos = videoPersistencePort.findVideosByArticleIds(articleIds);

        List<ArticleRawHtml> rawHtmls = articleHtmlPersistencePort.findRawHtmlByArticleIds(articleIds);
        List<ArticleLinks> articleLinks = rawHtmls.stream()
                .map(r -> topicsTagsExtractor.extract(r.articleId(), r.rawHtml()))
                .toList(); // adapter reads only .tags() from these

        exportPort.export(new ExportData(null, articles, images, videos, articleLinks,
                new TopicExportContext(target, page.articleTopics())));
    }
}
