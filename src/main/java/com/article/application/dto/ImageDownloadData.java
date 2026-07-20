package com.article.application.dto;

public record ImageDownloadData(
        String id,
        String articleId,
        String category,
        String imageUrl,
        Integer position
) {}
