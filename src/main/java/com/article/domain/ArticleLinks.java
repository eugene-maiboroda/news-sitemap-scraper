package com.article.domain;

import java.util.List;

public record ArticleLinks(
        String articleId,
        List<ArticleLinkValue> topics,
        List<ArticleLinkValue> tags
) {}
