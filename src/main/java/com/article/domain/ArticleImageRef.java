package com.article.domain;

public record ArticleImageRef(
        String articleId,
        String category,
        String imageUrl,
        Integer position
) {}