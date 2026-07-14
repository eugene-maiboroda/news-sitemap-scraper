package com.article;

import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.port.ArticlePersistencePort;

import java.util.List;

public class FakeArticlePersistence implements ArticlePersistencePort {

    private final List<ArticleLink> links;

    public FakeArticlePersistence(List<ArticleLink> links) {
        this.links = links;
    }

    @Override
    public void insertLinks(List<ArticleLink> links) {}

    @Override
    public List<ArticleLink> findNewBatch(int limit) {
        return links;
    }

    @Override
    public void saveArticleAndMarkParsed(Article article, String articleUrl) {
        System.out.println("PARSED: " + article.title());
    }

    @Override
    public void markError(String articleUrl, String errorMessage) {
        System.out.println("ERROR: " + articleUrl + " | " + errorMessage);
    }

    @Override
    public List<String> findDistinctCategories() {
        return List.of();
    }

    @Override
    public List<Article> findArticlesByCategory(String category, int page, int pageSize) {
        return List.of();
    }

    @Override
    public List<Article> findArticlesWithoutCategory(int page, int pageSize) {
        return List.of();
    }
}
