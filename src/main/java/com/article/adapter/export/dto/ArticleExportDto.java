package com.article.adapter.export.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

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
        List<ImageExportDto> images,
        List<VideoExportDto> videos,
        List<LinkDto> topics,
        List<LinkDto> tags,
        String content
) {}
