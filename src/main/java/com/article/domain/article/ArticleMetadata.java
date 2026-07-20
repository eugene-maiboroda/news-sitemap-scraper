package com.article.domain.article;

import java.util.List;

public record ArticleMetadata(
        List<TaxonomyLink> topics,
        List<TaxonomyLink> tags,
        String mainCategory
) {
    public ArticleMetadata {
        topics = topics == null ? List.of() : List.copyOf(topics);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
