package com.article.domain;

import com.article.adapter.export.dto.LinkDto;

import java.util.List;

public sealed interface ParseResult {

    record Success(

            Article article,

            List<Image> images,

            List<Video> videos
    ) implements ParseResult {
    }

    record HunterSuccess(

            Article article,

            String rawHtml,

            String affiliateDisclosure,

            List<LinkDto> tags,

            List<LinkDto> footerTags,

            List<LinkDto> relatedStories,

            List<Image> images,

            List<String> videoUrls
    ) implements ParseResult {
    }

    record Failure(

            ContentError error
    ) implements ParseResult {
    }

    static ParseResult ok(Article article, List<Image> images, List<Video> videos) {
        return new Success(article, images, videos);
    }

    static ParseResult error(String url, String reason) {
        return new Failure(new ContentError(url, reason));
    }
}
