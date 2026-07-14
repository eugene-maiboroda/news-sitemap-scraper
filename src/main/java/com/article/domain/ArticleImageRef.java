package com.article.domain;

public record ArticleImageRef(
        String id,
        String articleId,
        String category,
        String imageUrl,
        Integer position
) {}