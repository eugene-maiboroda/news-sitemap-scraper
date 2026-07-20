package com.article.domain;

import java.util.List;

public record ArticleMetadataResult(
        String articleId,
        List<ArticleLinkValue> topics,
        List<ArticleLinkValue> tags,
        String mainCategory
) {
}
