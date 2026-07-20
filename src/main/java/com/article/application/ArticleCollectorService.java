package com.article.application;

import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.StepsProps;
import com.article.port.in.ArticlePipelineUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.IntConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectorService implements ArticlePipelineUseCase {

    private final ParseArticlesUseCase parseArticlesUseCase;
    private final DiscoverLinksUseCase discoverLinksUseCase;
    private final DownloadImagesUseCase downloadImagesUseCase;
    private final DownloadVideosUseCase downloadVideosUseCase;
    private final AuthorUseCase authorUseCase;
    private final ExportArticlesUseCase exportArticlesUseCase;
    private final StepsProps steps;
    private final CollectorProps collector;

    @Override
    public void run() {

        try {
            runIfEnabled(
                    steps.discoverLinks(),
                    batchSize -> discoverLinksUseCase.discoverLinks(collector.sitemapUrls(), batchSize)
            );
            runIfEnabled(steps.parsePages(), parseArticlesUseCase::parsePages);
            runIfEnabled(steps.downloadImages(), downloadImagesUseCase::downloadImages);
            runIfEnabled(steps.downloadVideos(), downloadVideosUseCase::downloadVideos);
            runIfEnabled(steps.processAuthors(), authorUseCase::processAuthors);
            runIfEnabled(steps.exportAuthors(), authorUseCase::exportAuthors);
            runIfEnabled(steps.exportJson(), exportArticlesUseCase::exportArticles);

            log.info("NYPost Article Collector finished");
        } catch (Exception e) {
            log.error("NYPost Article Collector failed", e);
            throw new IllegalStateException("Article pipeline failed", e);
        }
    }

    private void runIfEnabled(StepsProps.Step step, IntConsumer action) {
        if (!step.enabled()) {
            return;
        }
        if (step.batchSize() <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than zero");
        }
        action.accept(step.batchSize());
    }

    private void runIfEnabled(StepsProps.ToggleStep step, Runnable action) {
        if (step.enabled()) {
            action.run();
        }
    }

}
