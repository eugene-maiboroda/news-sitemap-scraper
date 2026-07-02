package com.article.domain;

import java.util.List;

public sealed interface ParseResult {

    record Ok(Article article, List<Video> videos) implements ParseResult {}

    record Failure(ContentError error) implements ParseResult {}

    static ParseResult ok(Article article, List<Video> videos) {
        return new Ok(article, videos);
    }

    static ParseResult error(String url, String reason) {
        return new Failure(new ContentError(url, reason));
    }
}
