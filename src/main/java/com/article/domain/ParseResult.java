package com.article.domain;

import java.util.List;

public sealed interface ParseResult {

    record Success(Article article, List<Image> images, List<Video> videos) implements ParseResult {}

    record Failure(ContentError error) implements ParseResult {}

    static ParseResult ok(Article article, List<Image> images, List<Video> videos) {
        return new Success(article, images, videos);
    }

    static ParseResult error(String url, String reason) {
        return new Failure(new ContentError(url, reason));
    }
}
