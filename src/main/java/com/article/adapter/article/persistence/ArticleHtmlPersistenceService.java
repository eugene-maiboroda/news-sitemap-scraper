package com.article.adapter.article.persistence;

import com.article.domain.ArticleRawHtml;
import com.article.port.persistence.ArticleHtmlPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleHtmlPersistenceService implements ArticleHtmlPersistencePort {

    private final ObjectProvider<ArticleRepository> articleRepositoryProvider;

    @Override
    public List<ArticleRawHtml> findRawHtmlByArticleIds(List<String> articleIds) {
        return articleRepositoryProvider.getObject().findRawHtmlByIdIn(articleIds).stream()
                .map(view -> new ArticleRawHtml(view.getId(), view.getRawHtml()))
                .toList();
    }
}
