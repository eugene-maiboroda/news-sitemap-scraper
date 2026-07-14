package com.article.port;

import com.article.domain.ArticleRawHtml;

import java.util.List;

public interface ArticleHtmlPersistencePort {

    List<ArticleRawHtml> findRawHtmlByArticleIds(List<String> articleIds);
}
