package com.article.adapter.article.persistence;

import com.article.domain.ArticleRawHtml;
import com.article.port.ArticleHtmlPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleHtmlPersistenceService implements ArticleHtmlPersistencePort {

    private final ArticleRepository articleRepository;

    @Override
    public List<ArticleRawHtml> findRawHtmlByArticleIds(List<String> articleIds) {
        return articleRepository.findRawHtmlByIdIn(articleIds).stream()
                .map(view -> new ArticleRawHtml(view.getId(), view.getRawHtml()))
                .toList();
    }
}
