package com.article.port;

import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;

public interface ArticlePagePort {

    ParseResult parse(ArticleLink link);
}
