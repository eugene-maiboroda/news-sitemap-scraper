package com.article.application;

import com.article.bootstrap.properties.ExportPops;
import com.article.bootstrap.properties.StepsProps;
import com.article.domain.status.ExportMode;
import com.article.port.ExportPort;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.persistence.ArticleTopicsPersistencePort;
import com.article.port.persistence.ImagePersistencePort;
import com.article.port.persistence.VideoPersistencePort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportJsonUseCaseTest {

    @Test
    void routesCategoryModeToCategoryExport() {
        Dependencies dependencies = dependencies(ExportMode.CATEGORY);
        when(dependencies.articles.findDistinctCategories()).thenReturn(List.of());
        when(dependencies.articles.findArticlesWithoutCategory(0, 500)).thenReturn(List.of());

        dependencies.useCase.exportJson();

        verify(dependencies.articles).findDistinctCategories();
        verify(dependencies.articles).findArticlesWithoutCategory(0, 500);
    }

    @Test
    void routesTagsModeToOrderedArticleScan() {
        Dependencies dependencies = dependencies(ExportMode.TAGS);
        when(dependencies.articles.findAllArticlesOrdered(0, 500)).thenReturn(List.of());

        dependencies.useCase.exportJson();

        verify(dependencies.articles).findAllArticlesOrdered(0, 500);
    }

    @Test
    void routesTopicsModeToJsonbTopicDiscovery() {
        Dependencies dependencies = dependencies(ExportMode.TOPICS);
        when(dependencies.topics.findDistinctTopicTargets()).thenReturn(List.of());

        dependencies.useCase.exportJson();

        verify(dependencies.topics).findDistinctTopicTargets();
    }

    private Dependencies dependencies(ExportMode mode) {
        ArticlePersistencePort articles = mock(ArticlePersistencePort.class);
        VideoPersistencePort videos = mock(VideoPersistencePort.class);
        ImagePersistencePort images = mock(ImagePersistencePort.class);
        ArticleTopicsPersistencePort topics = mock(ArticleTopicsPersistencePort.class);
        ExportPort export = mock(ExportPort.class);
        StepsProps.Step disabled = new StepsProps.Step(false, 0);
        StepsProps steps = new StepsProps(
                disabled,
                disabled,
                disabled,
                disabled,
                disabled,
                new StepsProps.Step(true, 500),
                disabled
        );
        ExportJsonUseCase useCase = new ExportJsonUseCase(
                new ExportPops(mode),
                steps,
                articles,
                videos,
                images,
                topics,
                export
        );
        return new Dependencies(useCase, articles, topics);
    }

    private record Dependencies(
            ExportJsonUseCase useCase,
            ArticlePersistencePort articles,
            ArticleTopicsPersistencePort topics
    ) {}
}
