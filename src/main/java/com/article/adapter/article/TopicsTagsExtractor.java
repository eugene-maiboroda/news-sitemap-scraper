package com.article.adapter.article;

import com.article.domain.article.TaxonomyLink;
import com.article.domain.article.ArticleMetadata;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TopicsTagsExtractor {

    private static final String TOPICS_SELECTOR = ".article-header__top .flag.flag--stamp";
    private static final String TAGS_SELECTOR = ".article-footer .tag-list a[rel=tag]";

    private final MainCategoryResolver mainCategoryResolver;

    public ArticleMetadata extract(Document doc) {
        List<TaxonomyLink> topics = extractTopics(doc);
        TaxonomyLink firstTopic = topics.isEmpty() ? null : topics.get(0);
        String mainCategory = firstTopic == null
                ? null
                : mainCategoryResolver.resolve(firstTopic.name(), firstTopic.url());
        return new ArticleMetadata(topics, extractTags(doc), mainCategory);
    }

    private List<TaxonomyLink> extractTopics(Document doc) {
        List<TaxonomyLink> topics = new ArrayList<>();
        for (Element flag : doc.select(TOPICS_SELECTOR)) {
            String name = blankToNull(flag.text());
            String url = flagUrl(flag);
            if (name != null || url != null) {
                topics.add(new TaxonomyLink(name, url));
            }
        }
        return topics;
    }

    private List<TaxonomyLink> extractTags(Document doc) {
        List<TaxonomyLink> tags = new ArrayList<>();
        for (Element anchor : doc.select(TAGS_SELECTOR)) {
            String name = blankToNull(anchor.text());
            String url = blankToNull(anchor.absUrl("href"));
            if (name != null || url != null) {
                tags.add(new TaxonomyLink(name, url));
            }
        }
        return tags;
    }

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
