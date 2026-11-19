package com.article.port;

import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;
import com.article.domain.Image;

import java.util.List;

public interface ArticlePagePort {

    ParseResult parse(ArticleLink link);

    default ArchivedContent parseArchivedContent(String articleId, String articleUrl, String rawHtml) {
        throw new UnsupportedOperationException("Archive content rebuilding is not supported for this site");
    }

    record ArchivedContent(

            String content,

            List<Image> images
    ) {}

    List<Image> parseArchivedImages(String articleId, String articleUrl, String rawHtml);
}
