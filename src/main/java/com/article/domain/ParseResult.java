package com.article.domain;

public sealed interface ParseResult {

    record Ok(Article article) implements ParseResult {}

    record Failure(ContentError error) implements ParseResult {}

    static ParseResult ok(Article article) {
        return new Ok(article);
    }

    static ParseResult error(String url, String reason) {
        return new Failure(new ContentError(url, reason));
    }
}
