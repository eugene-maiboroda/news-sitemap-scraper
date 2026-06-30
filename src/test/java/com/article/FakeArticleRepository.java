package com.article;

import com.article.domain.Article;
import com.article.domain.ArticleImageRef;
import com.article.domain.ArticleLink;
import com.article.port.ArticleRepositoryPort;

import java.util.List;

public class FakeArticleRepository implements ArticleRepositoryPort {

    private final List<ArticleLink> links;

    public FakeArticleRepository(List<ArticleLink> links) {
        this.links = links;
    }

    @Override
    public List<ArticleLink> findUnparsedBatch(int limit) {
        return links;
    }

    @Override
    public void saveArticleAndMarkParsed(Article article, String articleUrl) {
        System.out.println("PARSED: " + article.title());
    }

    @Override
    public void insertLinks(List<ArticleLink> links) {
    }

    @Override
    public List<Article> findAllArticles() {
        return List.of();
    }

    @Override
    public List<ArticleImageRef> findImageRefsForDelivery(int limit) {
        return List.of();
    }
}