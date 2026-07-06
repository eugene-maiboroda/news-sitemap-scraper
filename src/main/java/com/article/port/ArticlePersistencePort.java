package com.article.port;

import com.article.domain.Article;
import com.article.domain.ArticleImageRef;
import com.article.domain.ArticleLink;

import java.util.List;

public interface ArticlePersistencePort {

    void insertLinks(List<ArticleLink> links);

    List<ArticleLink> findNewBatch(int limit);

    void saveArticleAndMarkParsed(Article article, String articleUrl);

    void markError(String articleUrl, String errorMessage);

    List<Article> findAllArticles();

    List<ArticleImageRef> findImageRefsForDelivery(int limit);
}
