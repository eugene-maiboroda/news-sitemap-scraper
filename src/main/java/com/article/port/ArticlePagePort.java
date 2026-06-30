package com.article.port;

import com.article.domain.Article;
import com.article.domain.ArticleLink;

public interface ArticlePagePort {

    Article parse(ArticleLink link);
}
