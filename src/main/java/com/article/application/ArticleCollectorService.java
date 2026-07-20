package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectorService implements ArticleCollectionUseCase {

    private final ParseArticleUseCase parseArticleUseCase;
    private final DiscoveryLinkUseCase discoveryLinkUseCase;
    private final ExtractTopicUseCase extractTopicUseCase;
    private final ImagesDownloaderUseCase imagesDownloaderUseCase;
    private final VideosDownloadUseCase videosDownloadUseCase;
    private final ExportJsonUseCase exportJsonUseCase;
    private final StepsProps steps;

    @Override
    public void run() {

        try {
            runIfEnabled(steps.discoverLinks(), discoveryLinkUseCase::discoverLinks);
            runIfEnabled(steps.parsePages(), parseArticleUseCase::parsePages);
            runIfEnabled(steps.extractMetadata(), extractTopicUseCase::extractTopics);
            runIfEnabled(steps.downloadImages(), imagesDownloaderUseCase::downloadImages);
            runIfEnabled(steps.downloadVideos(), videosDownloadUseCase::downloadVideo);
            runIfEnabled(steps.exportJson(), exportJsonUseCase::exportJson);
//        log.info("Author parsed and updated");
//        authorUseCase.parseAuthors();
//        log.info("Authors parsed and updated");

//        authorUseCase.downloadAvatars();
//        log.info("Author avatars downloaded");

//        authorUseCase.exportAuthors();
//        log.info("Authors exported");

            log.info("NYPost Article Collector finished");
        } catch (Exception e) {
            log.error("NYPost Article Collector failed", e);
        }
    }

    private void runIfEnabled(StepsProps.Step step, Runnable action) {
        if (step.enabled()) {
            action.run();
        }
    }

}
