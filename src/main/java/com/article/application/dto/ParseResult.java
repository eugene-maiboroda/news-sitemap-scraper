package com.article.application.dto;

import com.article.domain.article.Article;
import com.article.domain.article.ArticleMetadata;
import com.article.domain.media.Image;
import com.article.domain.media.Video;
import java.util.List;

public sealed interface ParseResult {

    record Success(
            Article article,
            ArticleMetadata metadata,
            List<Image> images,
            List<Video> videos
    ) implements ParseResult {}

    record Failure(ContentError error) implements ParseResult {}

    static ParseResult ok(
            Article article,
            ArticleMetadata metadata,
            List<Image> images,
            List<Video> videos
    ) {
        return new Success(article, metadata, images, videos);
    }

    static ParseResult error(String url, String reason) {
        return new Failure(new ContentError(url, reason));
    }
}
