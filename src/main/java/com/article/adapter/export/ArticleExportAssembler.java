package com.article.adapter.export;

import com.article.adapter.export.mapper.ArticleExportMapper;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.domain.article.ArticleMetadata;
import com.article.domain.article.ArticleWithMetadata;
import com.article.domain.media.Image;
import com.article.domain.media.Video;
import com.article.application.dto.ArticleExportBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ArticleExportAssembler {

    private final ArticleExportMapper articleExportMapper;

    public List<PreparedArticleExport> assemble(ArticleExportBatch batch) {
        Map<String, List<Image>> imagesByArticleId = batch.images().stream()
                .collect(Collectors.groupingBy(Image::articleId));
        Map<String, List<Video>> videosByArticleId = batch.videos().stream()
                .collect(Collectors.groupingBy(Video::articleId));

        return batch.articles().stream()
                .map(article -> prepare(article, imagesByArticleId, videosByArticleId))
                .toList();
    }

    private PreparedArticleExport prepare(ArticleWithMetadata source, Map<String, List<Image>> imagesByArticleId,
                                          Map<String, List<Video>> videosByArticleId) {

        String articleId = source.article().id();
        ArticleMetadata metadata = source.metadata();
        return new PreparedArticleExport(articleExportMapper.toDto(
                source.article(),
                imagesByArticleId.getOrDefault(articleId, List.of()),
                videosByArticleId.getOrDefault(articleId, List.of()),
                metadata.topics(),
                metadata.tags()
        ), metadata);
    }
}
