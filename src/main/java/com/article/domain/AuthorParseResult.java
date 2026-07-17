package com.article.domain;

public sealed interface AuthorParseResult {

    record Success(Author author) implements AuthorParseResult {}

    record Failure(ContentError error) implements AuthorParseResult {}

    static AuthorParseResult ok(Author author) {
        return new Success(author);
    }

    static AuthorParseResult error(String url, String reason) {
        return new Failure(new ContentError(url, reason));
    }
}
