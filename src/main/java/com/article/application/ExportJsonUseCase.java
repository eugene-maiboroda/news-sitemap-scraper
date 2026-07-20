package com.article.application;

import com.article.adapter.article.TopicsTagsExtractor;
import com.article.application.dto.ExportData;
import com.article.application.dto.TopicExportContext;
import com.article.bootstrap.properties.ExportPops;
import com.article.bootstrap.properties.StepsProps;
import com.article.domain.*;
import com.article.port.ExportPort;
import com.article.port.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExportJsonUseCase {

    private final ExportPops exportPops;
    private final StepsProps stepsProps;
    private final ArticlePersistencePort articlePersistencePort;
    private final ArticleHtmlPersistencePort articleHtmlPersistencePort;
    private final VideoPersistencePort videoPersistencePort;
    private final ImagePersistencePort imagePersistencePort;
    private final ArticleTopicsPersistencePort articleTopicsPersistencePort;
    private final ExportPort exportPort;
    private final TopicsTagsExtractor topicsTagsExtractor;

    public void exportJson() {

        String mode = exportPops.mode();
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

        int pageSize = stepsProps.exportJson().batchSize();

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
        int pageSize = stepsProps.exportJson().batchSize();
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
        int pageSize = stepsProps.exportJson().batchSize();
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
