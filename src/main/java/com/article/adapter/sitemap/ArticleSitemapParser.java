package com.article.adapter.sitemap;

import com.article.adapter.shared.ParseUtils;
import com.article.domain.ArticleLink;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ArticleSitemapParser {

    private static final String URL_SELECTOR = "url";
    private static final String LOC_SELECTOR = "loc";
    private static final String LASTMOD_SELECTOR = "lastmod";

    public List<ArticleLink> parseLinks(String quarterSitemapXml, String teamName) {
        return Jsoup.parse(quarterSitemapXml, ParseUtils.EMPTY, Parser.xmlParser())
                .select(URL_SELECTOR).stream()
                .map(urlEl -> toArticleLink(urlEl, teamName))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ArticleLink> toArticleLink(Element urlEl, String teamName) {
        String articleUrl = ParseUtils.getText(urlEl.select(LOC_SELECTOR));
        String lastmod = ParseUtils.getText(urlEl.select(LASTMOD_SELECTOR));
        try {
            return Optional.of(new ArticleLink(teamName, articleUrl, parseDate(lastmod)));
        } catch (Exception e) {
            log.warn("Failed to parse date for: {}", articleUrl);
            return Optional.empty();
        }
    }

    private LocalDate parseDate(String dateString) {
        if (dateString.contains("T")) {
            return LocalDate.parse(dateString.substring(0, 10));
        }
        return LocalDate.parse(dateString);
    }
}