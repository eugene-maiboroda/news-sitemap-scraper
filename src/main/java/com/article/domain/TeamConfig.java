package com.article.domain;

import java.time.LocalDate;

public record TeamConfig(String teamName, String teamSitemapUrl, LocalDate startDate, LocalDate endDate) {
}
