package com.article.adapter.sitemap;

import com.article.adapter.http.HttpContentLoader;
import com.article.domain.ArticleLink;
import com.article.port.SitemapPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SitemapService implements SitemapPort {

    private final HttpContentLoader contentLoader;

    @Override
    public List<ArticleLink> discoverLinks(String sitemapUrl) {
        Deque<String> pendingSitemaps = new ArrayDeque<>();
        Set<String> visitedSitemaps = new HashSet<>();
        Set<String> articleUrls = new LinkedHashSet<>();
        pendingSitemaps.add(sitemapUrl);

        while (!pendingSitemaps.isEmpty()) {
            String url = pendingSitemaps.removeFirst();
            if (!visitedSitemaps.add(url)) {
                continue;
            }

            try {
                readSitemap(url, pendingSitemaps, articleUrls);
            } catch (RuntimeException e) {
                log.warn("Failed to read sitemap {}", url, e);
            }
        }

        return articleUrls.stream()
                .map(url -> new ArticleLink(url, null))
                .toList();
    }

    private void readSitemap(String url, Deque<String> pendingSitemaps, Set<String> articleUrls) {
        String xml = contentLoader.loadText(url);
        Document document = Jsoup.parse(xml, url, Parser.xmlParser());
        Element root = document.firstElementChild();
        if (root == null) {
            throw new IllegalArgumentException("Empty sitemap");
        }

        switch (root.normalName()) {
            case "sitemapindex" -> addLocations(root, "sitemap", pendingSitemaps);
            case "urlset" -> addLocations(root, "url", articleUrls);
            default -> throw new IllegalArgumentException("Unsupported sitemap root: " + root.tagName());
        }
    }

    private static void addLocations(Element root, String entryTag, Collection<String> urls) {
        for (Element entry : root.children()) {
            if (!entry.normalName().equals(entryTag)) {
                continue;
            }

            Element location = entry.children().stream()
                    .filter(child -> child.normalName().equals("loc"))
                    .findFirst()
                    .orElse(null);
            if (location != null && !location.text().isBlank()) {
                urls.add(location.text());
            }
        }
    }
}
