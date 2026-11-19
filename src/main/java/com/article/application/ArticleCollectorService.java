package com.article.application;

import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.StepsProps;
import com.article.port.ArticleCollectionUseCase;
import com.article.port.ArticleJsonExportPort;
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
    private final ObjectProvider<ArticleJsonExportPort> articleJsonExportProvider;

    @Override
    public void run() {
        log.info("Article collector started | site={}", collector.site());
        try {
            if (steps.parsePages().enabled() && steps.parsePages().source() == StepsProps.ParseSource.RAW_HTML) {
                if (steps.parsePages().archiveOperation() == StepsProps.ArchiveOperation.MEDIA_CONTENT) {
                    parseArticleUseCase.rebuildArchivedContent();
                } else {
                    parseArticleUseCase.backfillCaptions();
                }
                return;
            }

            runIfEnabled(steps.discoverLinks(), discoveryLinkUseCase::discoverLinks);
            if (steps.parsePages().enabled()) {
                parseArticleUseCase.parsePages();
                stateDirectoryExportProvider.ifAvailable(StateDirectoryExportPort::export);
            }

            if (steps.extractAuthors() != null && steps.extractAuthors().enabled()) {
                authorUseCase.extractAuthors();
            }

            runIfEnabled(steps.downloadImages(), imagesDownloaderUseCase::downloadImages);
            runIfEnabled(steps.downloadVideos(), videosDownloadUseCase::downloadVideo);
            if (steps.exportJson().enabled()) {
                exportArticles();
            }
            log.info("Article collector finished");
        } catch (Exception e) {
            log.error("Article collector failed", e);
        }
    }

    private void exportArticles() {
        ArticleJsonExportPort exporter = articleJsonExportProvider.getIfAvailable();
        if (exporter == null) {
            throw new IllegalStateException("Article JSON export is not configured for the selected site");
        }
        exporter.exportArticles();
        log.info("Article JSON export finished");
    }

    private void runIfEnabled(StepsProps.Step step, Runnable action) {
        if (step.enabled()) {
            action.run();
        }
    }

}
