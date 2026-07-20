package com.article.port;

import com.article.domain.ArticleLinks;

public interface MetadataExtractorPort {

    ArticleLinks extract(String articleId, String rawHtml);
}
