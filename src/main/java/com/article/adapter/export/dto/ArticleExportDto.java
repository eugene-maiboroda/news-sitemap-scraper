package com.article.adapter.export.dto;

import com.article.domain.Image;
import com.article.domain.Video;
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
        List<String> images,
        List<String> videos,
        String imageCaption,
        String content
) {}
