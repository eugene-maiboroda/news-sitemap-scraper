package com.article.port.out.persistence;

import com.article.domain.article.Article;
import com.article.domain.article.ArticleLink;
import com.article.domain.article.ArticleMetadata;
import com.article.domain.article.ArticleWithMetadata;

import java.util.List;
import java.util.Map;

public interface ArticlePersistencePort {

    void insertLinks(List<ArticleLink> links);

    List<ArticleLink> findNewBatch(int limit);

    void saveArticleAndMarkParsed(Article article, ArticleMetadata metadata, String articleUrl);

    void markError(String articleUrl, String errorMessage);

    List<ArticleWithMetadata> findArticlesForExport(int page, int pageSize);

    Map<String, String> findCategoriesByArticleIds(List<String> articleIds);
}
