package com.article.adapter.export.dto;

public record AuthorExportDto(
        String authorName,
        String authorUrl,
        String bio,
        String avatarUrl,
        String avatarId
) {
}
