package com.article.domain;

import lombok.Builder;

import java.time.LocalDate;
@Builder
public record ArticleLink(String articleUrl, LocalDate articleDate) {}