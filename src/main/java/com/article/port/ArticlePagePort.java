package com.article.port;

import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;
import com.article.domain.Image;

import java.util.List;

public interface ArticlePagePort {

    ParseResult parse(ArticleLink link);

    List<Image> parseArchivedImages(String articleId, String articleUrl, String rawHtml);
}
