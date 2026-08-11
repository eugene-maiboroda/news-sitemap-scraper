package com.article.adapter.sitemap;

import com.article.adapter.http.HttpContentLoader;
import com.article.domain.ArticleLink;
import com.article.port.SitemapPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SitemapAdapter implements SitemapPort {

    private final HttpContentLoader contentLoader;
    private final QuarterSitemapParser quarterSitemapParser;
    private final ArticleSitemapParser articleSitemapParser;

    @Override
    public List<String> loadQuarterUrls(String teamSitemapUrl) {
        String xml = contentLoader.loadText(teamSitemapUrl);
        return quarterSitemapParser.extractQuarterUrls(xml);
    }

    @Override
    public List<ArticleLink> loadArticleLinks(String quarterSitemapUrl, String teamName) {
        String xml = contentLoader.loadText(quarterSitemapUrl);
        return articleSitemapParser.parseLinks(xml, teamName);
    }
}
