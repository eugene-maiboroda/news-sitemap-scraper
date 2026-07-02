package com.article.domain;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record Article(
        String id,
        String category,
        String url,
        LocalDate publishDate,
        String title,
        String subTitle,
        String author,
        String authorLink,
        String content,
        String imageUrl,
        String caption,
        String rawHtml
) {}
