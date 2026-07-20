package com.article.domain.article;

import lombok.Builder;

import java.time.LocalDate;
@Builder
public record ArticleLink(String articleUrl, LocalDate articleDate) {}
