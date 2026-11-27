package com.article.application;

import com.article.domain.article.ArticleLink;
import com.article.application.dto.ParseResult;
import com.article.port.out.ArticleParserPort;
import com.article.port.out.persistence.ArticlePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParseArticlesUseCase {

    private final ArticlePersistencePort articlePersistencePort;
    private final ArticleParserPort articleParserPort;
    private final ParallelExecutor parallelExecutor;
    private final ParsedMediaService parsedMediaService;
    private final PipelineRunSettings runSettings;

    public void parsePages(int batchSize) {
        log.info("Step: PARSE PAGES");
        log.info("Parsing pages | batchSize={}", batchSize);

        List<ArticleLink> batch;

        while (!runSettings.isStopRequested()
                && !(batch = articlePersistencePort.findNewBatch(batchSize)).isEmpty()) {
            parallelExecutor.execute(batch, this::parseSingle);
        }
        if (runSettings.isStopRequested()) {
            log.info("Parsing stopped");
        } else {
            log.info("No more links to parse");
        }
    }

    private void parseSingle(ArticleLink link) {
        long start = System.currentTimeMillis();
        switch (articleParserPort.parse(link)) {
            case ParseResult.Success success -> {
                articlePersistencePort.saveArticleAndMarkParsed(success.article(), success.metadata(), link.articleUrl());
                parsedMediaService.saveImages(success.images());
                parsedMediaService.saveVideos(success.videos());
                log.info("[PARSED] {} ({} ms)", link.articleUrl(), System.currentTimeMillis() - start);
            }
            case ParseResult.Failure f -> {
                articlePersistencePort.markError(link.articleUrl(), f.error().reason());
                log.warn("[ERROR] {} | {} ({} ms)",
                        link.articleUrl(), f.error().reason(), System.currentTimeMillis() - start);
            }
        }
    }
}
