package com.article.port;

import com.article.domain.ArticleLink;

import java.util.List;

public interface SitemapPort {

    List<String> loadQuarterUrls(String teamSitemapUrl);

    List<ArticleLink> loadArticleLinks(String quarterSitemapUrl, String teamName);

}