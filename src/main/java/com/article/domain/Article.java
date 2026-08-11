package com.article.domain;

import lombok.Builder;

import java.time.LocalDate;
@Builder
public record Article(
        String id,
        String team,
        String url,
        LocalDate publishDate,
        String title,
        String subTitle,
        String author,
        String authorLink,
        String content,
        String imageUrl,
        String caption
) {
}
