package com.article.domain;

import java.util.List;

public sealed interface ParseResult {

    record MenuArticleSuccess(

            Article article,

            String rawHtml,

            String affiliateDisclosure,

            List<NamedLink> tags,

            List<NamedLink> footerTags,

            List<NamedLink> relatedStories,

            List<Image> images,

            List<String> videoUrls
    ) implements ParseResult {
    }

    record Failure(

            ContentError error
    ) implements ParseResult {
    }

    static ParseResult error(String url, String reason) {
        return new Failure(new ContentError(url, reason));
    }
}
