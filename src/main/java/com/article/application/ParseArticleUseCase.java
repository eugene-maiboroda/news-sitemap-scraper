package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;
import com.article.port.ArticlePagePort;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.persistence.ParsedArticleStorePort;
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
    private final ObjectProvider<ParsedArticleStorePort> parsedArticleStoreProvider;

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
            case ParseResult.MenuArticleSuccess success -> saveMenuArticle(link.articleUrl(), success, start);
            case ParseResult.Failure failure -> {
                articlePersistencePort.markError(link.articleUrl(), failure.error().reason());
                log.warn("[ERROR] {} | {} ({} ms)", link.articleUrl(), failure.error().reason(), System.currentTimeMillis() - start);
            }
        }
    }

    private void saveMenuArticle(String url, ParseResult.MenuArticleSuccess success, long start) {
        try {
            parsedArticleStoreProvider.getObject().save(success);
            log.info("[PARSED] {} ({} ms)", url, System.currentTimeMillis() - start);
        } catch (DataAccessException e) {
            articlePersistencePort.markError(url, "Failed to persist Hunter article");
            log.warn("[ERROR] Failed to persist menu article {}", url, e);
        }
    }
}
