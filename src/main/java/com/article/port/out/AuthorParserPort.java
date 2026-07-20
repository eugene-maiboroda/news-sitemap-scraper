package com.article.port.out;

import com.article.domain.author.Author;
import com.article.application.dto.AuthorParseResult;

public interface AuthorParserPort {

    AuthorParseResult parse(Author author);
}
