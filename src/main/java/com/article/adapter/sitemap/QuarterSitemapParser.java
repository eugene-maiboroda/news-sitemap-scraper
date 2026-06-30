package com.article.adapter.sitemap;

import com.article.adapter.shared.ParseUtils;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuarterSitemapParser {

    private static final String LOC_SELECTOR = "loc";
    private static final String QUARTER_MARKER = "articles-sitemap.xml?quarter_year=";

    public List<String> extractQuarterUrls(String teamSitemapXml) {
        return Jsoup.parse(teamSitemapXml, ParseUtils.EMPTY, Parser.xmlParser())
                .select(LOC_SELECTOR).stream()
                .map(ParseUtils::getText)
                .filter(url -> url.contains(QUARTER_MARKER))
                .toList();
    }
}
