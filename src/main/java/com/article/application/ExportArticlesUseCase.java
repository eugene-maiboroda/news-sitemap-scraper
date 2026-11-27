package com.article.application;

import com.article.domain.article.Article;
import com.article.domain.article.ArticleWithMetadata;
import com.article.domain.media.Image;
import com.article.domain.media.Video;
import com.article.application.dto.ArticleExportBatch;
import com.article.port.out.ArticleExportPort;
import com.article.port.out.persistence.ArticlePersistencePort;
import com.article.port.out.persistence.ImagePersistencePort;
import com.article.port.out.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExportArticlesUseCase {

    private final ArticlePersistencePort articlePersistencePort;
    private final VideoPersistencePort videoPersistencePort;
    private final ImagePersistencePort imagePersistencePort;
    private final ArticleExportPort articleExportPort;
    private final PipelineRunSettings runSettings;

    public void exportArticles(int batchSize) {
        log.info("Step: EXPORT JSON");
        int page = 0;
        Throwable failure = null;
        try {
            List<ArticleWithMetadata> batch;
            while (!runSettings.isStopRequested()
                    && !(batch = articlePersistencePort.findArticlesForExport(page, batchSize)).isEmpty()) {
                exportBatch(batch);
                page++;
            }
        } catch (RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            completeExport(failure);
        }
        if (runSettings.isStopRequested()) {
            log.info("Export stopped | pages={}", page);
        } else {
            log.info("Export finished | pages={}", page);
        }
    }

    private void exportBatch(List<ArticleWithMetadata> batch) {
        List<Article> articles = batch.stream().map(ArticleWithMetadata::article).toList();
        List<String> articleIds = articles.stream().map(Article::id).toList();
        List<Image> images = imagePersistencePort.findImagesByArticleIds(articleIds);
        List<Video> videos = videoPersistencePort.findVideosByArticleIds(articleIds);
        articleExportPort.export(new ArticleExportBatch(batch, images, videos));
    }

    private void completeExport(Throwable failure) {
        try {
            articleExportPort.complete();
        } catch (RuntimeException | Error e) {
            if (failure == null) {
                throw e;
            }
            if (failure != e) {
                failure.addSuppressed(e);
            }
        }
    }
}
