package com.article.domain;

import java.util.List;

/**
 * One page of articles matching a {@link TopicTarget}. Each item carries the article and its
 * already-mapped JSONB metadata from the same database row.
 */
public record ArticleTopicsPage(
        List<ArticleWithMetadata> articles
) {}
