package com.article.port;

import com.article.domain.ArticleLink;

import java.util.List;

public interface SitemapPort {

    List<ArticleLink> discoverLinks(String sitemapUrl);
}
