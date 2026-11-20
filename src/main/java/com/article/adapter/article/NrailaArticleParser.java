package com.article.adapter.article;

import com.article.domain.ArticleLink;
import com.article.domain.Image;
import com.article.domain.ParseResult;
import com.article.port.ArticlePagePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "collector.site", havingValue = "NRAILA")
public class NrailaArticleParser implements ArticlePagePort {

    @Override
    public ParseResult parse(ArticleLink link) {
        throw new UnsupportedOperationException("NRA-ILA parsing is not implemented yet");
    }

    @Override
    public List<Image> parseArchivedImages(String articleId, String articleUrl, String rawHtml) {
        throw new UnsupportedOperationException("NRA-ILA archive parsing is not implemented yet");
    }
}
