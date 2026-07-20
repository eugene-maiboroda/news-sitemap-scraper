package com.article.adapter.article;

import com.article.domain.ArticleLinkValue;
import com.article.domain.ArticleLinks;
import com.article.port.MetadataExtractorPort;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * .article-header__top wraps a variable number of nested containers depending on the article
 * (a single flags-wrapper div in some layouts, direct anchors/divs in others) — the stable marker
 * across layouts is the "flag" class on each individual topic badge, not the DOM depth at which
 * it appears, so topics are matched by class rather than by direct-child position.
 *
 * The header also carries non-navigation badges that share the generic "flag" class: the author/
 * columnist badge ("flag--columnist") and franchise/promo badges ("flag--franchise-template", e.g.
 * a "Swing States 2024" feature badge). Only real navigation topics carry the "flag--stamp" token
 * (in any of its "--display"/"--alt"/"--display-interior"/"--sports-plus" modifier variants), so
 * that token is required in addition to "flag" to exclude those decorations.
 */
@Component
public class TopicsTagsExtractor implements MetadataExtractorPort {

    private static final String TOPICS_SELECTOR = ".article-header__top .flag.flag--stamp";
    private static final String TAGS_SELECTOR = ".article-footer .tag-list a[rel=tag]";

    @Override
    public ArticleLinks extract(String articleId, String rawHtml) {
        Document doc = Jsoup.parse(rawHtml == null ? "" : rawHtml);
        return new ArticleLinks(articleId, extractTopics(doc), extractTags(doc));
    }

    private List<ArticleLinkValue> extractTopics(Document doc) {
        List<ArticleLinkValue> topics = new ArrayList<>();
        for (Element flag : doc.select(TOPICS_SELECTOR)) {
            String name = blankToNull(flag.text());
            String url = flagUrl(flag);
            if (name != null || url != null) {
                topics.add(new ArticleLinkValue(name, url));
            }
        }
        return topics;
    }

    private List<ArticleLinkValue> extractTags(Document doc) {
        List<ArticleLinkValue> tags = new ArrayList<>();
        for (Element anchor : doc.select(TAGS_SELECTOR)) {
            String name = blankToNull(anchor.text());
            String url = blankToNull(anchor.absUrl("href"));
            if (name != null || url != null) {
                tags.add(new ArticleLinkValue(name, url));
            }
        }
        return tags;
    }

    // name and url are extracted independently — no inventing a name from aria-label or other
    // attributes when there's no visible text (e.g. an icon-only badge with just an <svg>).
    private String flagUrl(Element flag) {
        if ("a".equalsIgnoreCase(flag.tagName())) {
            return blankToNull(flag.absUrl("href"));
        }
        Element anchor = flag.selectFirst("a");
        return anchor != null ? blankToNull(anchor.absUrl("href")) : null;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
