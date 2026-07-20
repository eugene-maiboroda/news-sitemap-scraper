package com.article;

import com.article.domain.article.Article;
import com.article.domain.article.ArticleLink;
import com.article.domain.article.ArticleMetadata;
import com.article.domain.article.ArticleWithMetadata;
import com.article.port.out.persistence.ArticlePersistencePort;

import java.util.List;
import java.util.Map;

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
    public void saveArticleAndMarkParsed(
            Article article,
            ArticleMetadata metadata,
            String articleUrl
    ) {
        System.out.println("PARSED: " + article.title());
    }

    @Override
    public void markError(String articleUrl, String errorMessage) {
        System.out.println("ERROR: " + articleUrl + " | " + errorMessage);
    }

    @Override
    public List<ArticleWithMetadata> findArticlesForExport(int page, int pageSize) {
        return List.of();
    }

    @Override
    public Map<String, String> findCategoriesByArticleIds(List<String> articleIds) {
        return Map.of();
    }
}
