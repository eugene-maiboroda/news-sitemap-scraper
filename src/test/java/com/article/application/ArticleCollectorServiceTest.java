package com.article.application;

import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.StepsProps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ArticleCollectorServiceTest {

    @Test
    void passesConfiguredBatchSizeOnlyToEnabledSteps() {
        ParseArticlesUseCase parseArticles = mock(ParseArticlesUseCase.class);
        DiscoverLinksUseCase discoverLinks = mock(DiscoverLinksUseCase.class);
        DownloadImagesUseCase downloadImages = mock(DownloadImagesUseCase.class);
        DownloadVideosUseCase downloadVideos = mock(DownloadVideosUseCase.class);
        AuthorUseCase authors = mock(AuthorUseCase.class);
        ExportArticlesUseCase exportArticles = mock(ExportArticlesUseCase.class);
        CollectorProps collector = new CollectorProps(4, 0, List.of("https://example.com/sitemap.xml"), null);
        StepsProps steps = new StepsProps(
                enabled(100),
                enabled(200),
                disabled(),
                enabled(300),
                enabled(400),
                toggle(true),
                enabled(500),
                toggle(false)
        );

        new ArticleCollectorService(
                parseArticles,
                discoverLinks,
                downloadImages,
                downloadVideos,
                authors,
                exportArticles,
                steps,
                collector
        ).run();

        verify(discoverLinks).discoverLinks(collector.sitemapUrls(), 100);
        verify(parseArticles).parsePages(200);
        verify(downloadVideos).downloadVideos(300);
        verify(authors).processAuthors(400);
        verify(authors).exportAuthors();
        verify(exportArticles).exportArticles(500);
        verifyNoInteractions(downloadImages);
    }

    private StepsProps.Step enabled(int batchSize) {
        return new StepsProps.Step(true, batchSize);
    }

    private StepsProps.Step disabled() {
        return new StepsProps.Step(false, 0);
    }

    private StepsProps.ToggleStep toggle(boolean enabled) {
        return new StepsProps.ToggleStep(enabled);
    }
}
