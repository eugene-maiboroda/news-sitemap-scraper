package com.article.adapter.sitemap;

import com.article.adapter.shared.ParseUtils;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class NyPostYearSitemapParser {

    public List<String> extractDayUrls(String sitemapIndexXml) {
        return Jsoup.parse(sitemapIndexXml, ParseUtils.EMPTY, Parser.xmlParser())
                .select("sitemap > loc").stream()
                .map(el -> el.text().trim())
                .filter(url -> !url.isBlank())
                .toList();
    }
}
