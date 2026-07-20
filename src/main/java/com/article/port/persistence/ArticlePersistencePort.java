package com.article.port.persistence;

import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.domain.ArticleWithMetadata;
import com.article.domain.Author;

import java.util.List;

public interface ArticlePersistencePort {

    void insertLinks(List<ArticleLink> links);

    List<ArticleLink> findNewBatch(int limit);

    void saveArticleAndMarkParsed(Article article, String articleUrl);

    void markError(String articleUrl, String errorMessage);

    List<String> findDistinctCategories();

    List<ArticleWithMetadata> findArticlesByCategory(String category, int page, int pageSize);

    List<ArticleWithMetadata> findArticlesWithoutCategory(int page, int pageSize);

    List<ArticleWithMetadata> findAllArticlesOrdered(int page, int pageSize);

    List<Author> findDistinctAuthors();
}
