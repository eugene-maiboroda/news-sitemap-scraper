package com.article.port.persistence;

import com.article.domain.ParseResult;
import com.article.domain.Image;

import java.util.List;

public interface ParsedArticlePersistencePort {

    void save(ParseResult.MenuArticleSuccess result);

    List<ArchivedArticle> findCaptionBackfillBatch(String afterId, int limit);

    int fillMissingCaption(Image image);

    record ArchivedArticle(

            String id,

            String url,

            String rawHtml
    ) {
    }
}
