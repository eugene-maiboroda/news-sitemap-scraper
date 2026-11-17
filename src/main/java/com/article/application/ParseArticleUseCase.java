package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleLink;
import com.article.domain.Image;
import com.article.domain.ParseResult;
import com.article.port.ArticlePagePort;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.persistence.ParsedArticlePersistencePort;
import com.article.port.persistence.ParsedArticlePersistencePort.ArchivedArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParseArticleUseCase {

    private static final int MAX_ARCHIVE_BATCH_SIZE = 100;

    private final StepsProps stepsProps;
    private final ArticlePersistencePort articlePersistencePort;
    private final ArticlePagePort articlePagePort;
    private final ParallelExecutor parallelExecutor;
    private final ParsedArticlePersistencePort parsedArticlePersistence;

    public void parsePages() {
        log.info("Step: PARSE PAGES");

        int batchSize = stepsProps.parsePages().batchSize();
        log.info("Parsing pages | batchSize={}", batchSize);

        List<ArticleLink> batch;

        while (!(batch = articlePersistencePort.findNewBatch(batchSize)).isEmpty()) {
            parallelExecutor.execute(batch, this::parseSingle);
        }
        log.info("No more links to parse");
    }

    public void backfillCaptions() {
        int limit = Math.clamp(stepsProps.parsePages().batchSize(), 1, MAX_ARCHIVE_BATCH_SIZE);
        LongAdder updated = new LongAdder();
        LongAdder failed = new LongAdder();
        long scanned = 0;
        String afterId = "";
        List<ArchivedArticle> batch;

        while (!(batch = parsedArticlePersistence.findCaptionBackfillBatch(afterId, limit)).isEmpty()) {
            parallelExecutor.execute(batch, article -> {
                try {
                    for (Image image : articlePagePort.parseArchivedImages(article.id(), article.url(), article.rawHtml())) {
                        if (image.getCaption() != null && !image.getCaption().isBlank()) {
                            updated.add(parsedArticlePersistence.fillMissingCaption(image));
                        }
                    }
                } catch (RuntimeException e) {
                    failed.increment();
                    log.error("[RAW_HTML] Failed to fill captions | articleId={}", article.id(), e);
                }
            });
            scanned += batch.size();
            afterId = batch.getLast().id();
            log.info("[RAW_HTML] Caption backfill | articles={} captionsUpdated={} failed={}", scanned, updated.sum(), failed.sum());
        }

        log.info("[RAW_HTML] Caption backfill finished | articles={} captionsUpdated={} failed={}",
                scanned, updated.sum(), failed.sum());
    }

    private void parseSingle(ArticleLink link) {
        long start = System.currentTimeMillis();
        switch (articlePagePort.parse(link)) {
            case ParseResult.MenuArticleSuccess success -> saveMenuArticle(link.articleUrl(), success, start);
            case ParseResult.Failure failure -> {
                articlePersistencePort.markError(link.articleUrl(), failure.error().reason());
                log.warn("[ERROR] {} | {} ({} ms)", link.articleUrl(), failure.error().reason(), System.currentTimeMillis() - start);
            }
        }
    }

    private void saveMenuArticle(String url, ParseResult.MenuArticleSuccess success, long start) {
        try {
            parsedArticlePersistence.save(success);
            log.info("[PARSED] {} ({} ms)", url, System.currentTimeMillis() - start);
        } catch (DataAccessException e) {
            articlePersistencePort.markError(url, "Failed to persist article");
            log.warn("[ERROR] Failed to persist menu article {}", url, e);
        }
    }
}
