package com.article.port;

import com.article.domain.Author;
import com.article.domain.AuthorParseResult;

public interface AuthorParserPort {

    AuthorParseResult parse(Author author);
}
