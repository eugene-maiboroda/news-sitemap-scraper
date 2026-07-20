package com.article.domain.article;

public record ArticleWithMetadata(
        Article article,
        ArticleMetadata metadata
) {}
