package com.article.application;

import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.StepsProps;
import com.article.port.ArticleCollectionUseCase;
import com.article.port.StateDirectoryExportPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectorService implements ArticleCollectionUseCase {

    private final AuthorUseCase authorUseCase;
    private final ParseArticleUseCase parseArticleUseCase;
    private final DiscoveryLinkUseCase discoveryLinkUseCase;
    private final ImagesDownloaderUseCase imagesDownloaderUseCase;
    private final VideosDownloadUseCase videosDownloadUseCase;
    private final StepsProps steps;
    private final CollectorProps collector;
    private final ObjectProvider<StateDirectoryExportPort> stateDirectoryExportProvider;

    @Override
    public void run() {
        try {
            if (steps.exportJson().enabled()) {
                throw new IllegalStateException("Article JSON export is not configured for NRA");
            }

            runIfEnabled(steps.discoverLinks(), discoveryLinkUseCase::discoverLinks);
            runIfEnabled(steps.parsePages(), parseArticleUseCase::parsePages);

            if (steps.extractAuthors() != null && steps.extractAuthors().enabled()) {
                authorUseCase.extractAuthors();
            }

            if (collector.site() == CollectorProps.Site.AMERICAN_HUNTER) {
                runIfEnabled(steps.parsePages(), () -> stateDirectoryExportProvider.getObject().export());
                log.info("American Hunter Article Collector finished");
                return;
            }
            runIfEnabled(steps.downloadImages(), imagesDownloaderUseCase::downloadImages);
            runIfEnabled(steps.downloadVideos(), videosDownloadUseCase::downloadVideo);
            log.info("Article collector finished");
        } catch (Exception e) {
            log.error("Article collector failed", e);
        }
    }

    private void runIfEnabled(StepsProps.Step step, Runnable action) {
        if (step.enabled()) {
            action.run();
        }
    }

}
