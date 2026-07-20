package com.article.application.dto;

import com.article.domain.article.ArticleWithMetadata;
import com.article.domain.media.Image;
import com.article.domain.media.Video;

import java.util.List;

public record ArticleExportBatch(
        List<ArticleWithMetadata> articles,
        List<Image> images,
        List<Video> videos
) {
}
