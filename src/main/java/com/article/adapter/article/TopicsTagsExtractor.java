package com.article.adapter.article;

import com.article.adapter.export.dto.LinkDto;
import com.article.domain.ArticleLinks;
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
public class TopicsTagsExtractor {

    private static final String TOPICS_SELECTOR = ".article-header__top .flag.flag--stamp";
    private static final String TAGS_SELECTOR = ".article-footer .tag-list a[rel=tag]";

    public ArticleLinks extract(String articleId, String rawHtml) {
        Document doc = Jsoup.parse(rawHtml == null ? "" : rawHtml);
        return new ArticleLinks(articleId, extractTopics(doc), extractTags(doc));
    }

    private List<LinkDto> extractTopics(Document doc) {
        List<LinkDto> topics = new ArrayList<>();
        for (Element flag : doc.select(TOPICS_SELECTOR)) {
            String name = blankToNull(flag.text());
            String url = flagUrl(flag);
            if (name != null || url != null) {
                topics.add(new LinkDto(name, url));
            }
        }
        return topics;
    }

    private List<LinkDto> extractTags(Document doc) {
        List<LinkDto> tags = new ArrayList<>();
        for (Element anchor : doc.select(TAGS_SELECTOR)) {
            tags.add(new LinkDto(anchor.text(), anchor.absUrl("href")));
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
