package com.article.port.out;

import com.article.domain.article.ArticleLink;
import com.article.application.dto.ParseResult;

public interface ArticleParserPort {

    ParseResult parse(ArticleLink link);
}
