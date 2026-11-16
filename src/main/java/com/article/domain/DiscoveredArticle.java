package com.article.domain;

import java.util.List;
import java.util.Map;

public record DiscoveredArticle(
        String articleUrl,
        String title,
        String description,
        Map<String, List<List<String>>> menuPaths
) {}
