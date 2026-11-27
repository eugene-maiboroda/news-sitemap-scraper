package com.article.application;

import com.article.port.out.ArticleExportPort;
import com.article.port.out.persistence.ArticlePersistencePort;
import com.article.port.out.persistence.ImagePersistencePort;
import com.article.port.out.persistence.VideoPersistencePort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportArticlesUseCaseTest {

    @Test
    void scansArticlesOnceAndCompletesExport() {
        ArticlePersistencePort articles = mock(ArticlePersistencePort.class);
        VideoPersistencePort videos = mock(VideoPersistencePort.class);
        ImagePersistencePort images = mock(ImagePersistencePort.class);
        ArticleExportPort export = mock(ArticleExportPort.class);
        when(articles.findArticlesForExport(0, 500)).thenReturn(List.of());

        new ExportArticlesUseCase(articles, videos, images, export, mock(PipelineRunSettings.class)).exportArticles(500);

        verify(articles).findArticlesForExport(0, 500);
        verify(export).complete();
    }

    @Test
    void completesExportWhenBatchLoadingFails() {
        ArticlePersistencePort articles = mock(ArticlePersistencePort.class);
        VideoPersistencePort videos = mock(VideoPersistencePort.class);
        ImagePersistencePort images = mock(ImagePersistencePort.class);
        ArticleExportPort export = mock(ArticleExportPort.class);
        when(articles.findArticlesForExport(0, 500))
                .thenThrow(new IllegalStateException("database unavailable"));

        ExportArticlesUseCase useCase = new ExportArticlesUseCase(articles, videos, images, export, mock(PipelineRunSettings.class));

        assertThrows(IllegalStateException.class, () -> useCase.exportArticles(500));
        verify(export).complete();
    }
}
