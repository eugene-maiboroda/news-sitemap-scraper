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
class NyPostDaySitemapParser {

    public List<ArticleLink> parseLinks(String urlsetXml) {
        return Jsoup.parse(urlsetXml, ParseUtils.EMPTY, Parser.xmlParser())
                .select("url").stream()
                .map(this::toArticleLink)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ArticleLink> toArticleLink(Element urlEl) {
        String articleUrl = ParseUtils.getText(urlEl.select("loc"));
        String lastmod = ParseUtils.getText(urlEl.select("lastmod"));
        try {
            return Optional.of(new ArticleLink(articleUrl, parseDate(lastmod)));
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
