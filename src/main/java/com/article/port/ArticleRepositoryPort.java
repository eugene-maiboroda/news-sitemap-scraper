package com.article.port;

import com.article.domain.Article;
import com.article.domain.ArticleImageRef;
import com.article.domain.ArticleLink;

import java.util.List;

public interface ArticleRepositoryPort {

    void insertLinks(List<ArticleLink> links);

    List<ArticleLink> findUnparsedBatch(int limit);

    void saveArticleAndMarkParsed(Article article, String articleUrl);

    List<Article> findAllArticles();

    List<ArticleImageRef> findImageRefsForDelivery(int limit);
}