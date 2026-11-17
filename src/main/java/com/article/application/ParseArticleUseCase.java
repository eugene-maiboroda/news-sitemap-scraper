package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.adapter.article.persistence.HunterArticleStore;
import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;
import com.article.port.ArticlePagePort;
import com.article.port.persistence.ArticlePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParseArticleUseCase {

    private final StepsProps stepsProps;
    private final ArticlePersistencePort articlePersistencePort;
    private final ArticlePagePort articlePagePort;
    private final ParallelExecutor parallelExecutor;
    private final ParsedMediaService parsedMediaService;
    private final ObjectProvider<HunterArticleStore> hunterArticleStoreProvider;

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

    private void parseSingle(ArticleLink link) {
        long start = System.currentTimeMillis();
        switch (articlePagePort.parse(link)) {
            case ParseResult.Success success -> {
                articlePersistencePort.saveArticleAndMarkParsed(success.article(), link.articleUrl());
                parsedMediaService.saveImages(success.images());
                parsedMediaService.saveVideos(success.videos());
                log.info("[PARSED] {} ({} ms)", link.articleUrl(), System.currentTimeMillis() - start);
            }
            case ParseResult.HunterSuccess success -> {
                try {
                    hunterArticleStoreProvider.getObject().save(success);
                    log.info("[PARSED] {} ({} ms)", link.articleUrl(), System.currentTimeMillis() - start);
                } catch (DataAccessException e) {
                    articlePersistencePort.markError(link.articleUrl(), "Failed to persist Hunter article");
                    log.warn("[ERROR] Failed to persist Hunter article {}", link.articleUrl(), e);
                }
            }
            case ParseResult.Failure failure -> {
                articlePersistencePort.markError(link.articleUrl(), failure.error().reason());
                log.warn("[ERROR] {} | {} ({} ms)", link.articleUrl(), failure.error().reason(), System.currentTimeMillis() - start);
            }
        }
    }
}
