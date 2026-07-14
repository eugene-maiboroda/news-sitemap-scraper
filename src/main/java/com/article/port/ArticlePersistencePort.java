package com.article.port;

import com.article.domain.Article;
import com.article.domain.ArticleLink;

import java.util.List;

public interface ArticlePersistencePort {

    void insertLinks(List<ArticleLink> links);

    List<ArticleLink> findNewBatch(int limit);

    void saveArticleAndMarkParsed(Article article, String articleUrl);

    void markError(String articleUrl, String errorMessage);

    List<String> findDistinctCategories();

    List<Article> findArticlesByCategory(String category, int page, int pageSize);

    List<Article> findArticlesWithoutCategory(int page, int pageSize);
}
