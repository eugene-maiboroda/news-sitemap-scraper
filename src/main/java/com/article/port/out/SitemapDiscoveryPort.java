package com.article.port.out;

import com.article.domain.article.ArticleLink;

import java.util.List;

public interface SitemapDiscoveryPort {

    List<ArticleLink> discoverLinks(String sitemapUrl);
}
