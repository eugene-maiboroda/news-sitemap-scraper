package com.article.domain;

public record ArticleWithMetadata(
        Article article,
        ArticleLinks metadata
) {}
