package com.article.adapter.export.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ArticleExportDto(
        String id,
        String category,
        String articleUrl,
        String title,
        String subTitle,
        LocalDate publishedDate,
        String author,
        String authorLink,
        String imageId,
        String imageCaption,
        String content
) {}
