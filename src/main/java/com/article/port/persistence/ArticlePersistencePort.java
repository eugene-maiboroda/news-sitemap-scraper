package com.article.port.persistence;

import com.article.domain.ArticleLink;
import com.article.domain.Author;
import com.article.domain.DiscoveredArticle;

import java.util.List;

public interface ArticlePersistencePort {

    void insertLinks(List<ArticleLink> links);

    void saveDiscoveredArticles(List<DiscoveredArticle> articles);

    List<ArticleLink> findNewBatch(int limit);

    void markError(String articleUrl, String errorMessage);

    List<Author> findDistinctAuthors();
}
