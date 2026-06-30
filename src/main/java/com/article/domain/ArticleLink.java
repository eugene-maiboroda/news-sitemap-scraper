package com.article.domain;

import java.time.LocalDate;

public record ArticleLink(String teamName, String articleUrl, LocalDate articleDate) {}