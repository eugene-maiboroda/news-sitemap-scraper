package com.article.adapter.article.persistence;

import com.article.adapter.article.mapper.ArticleMapper;
import com.article.domain.Article;
import com.article.domain.ArticleImageRef;
import com.article.domain.ArticleLink;
import com.article.port.ArticleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleRepositoryService implements ArticleRepositoryPort {

    private final ArticleLinkRepository linkRepository;
    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    @Override
    @Transactional
    public void insertLinks(List<ArticleLink> links) {
        links.stream()
                .filter(link -> !linkRepository.existsByArticleUrl(link.articleUrl()))
                .map(articleMapper::toLinkEntity)
                .forEach(linkRepository::save);
    }

    @Override
    public List<ArticleLink> findUnparsedBatch(int limit) {
        return articleMapper.toArticleLinks(linkRepository.findUnparsedBatch(PageRequest.of(0, limit)));
    }

    @Override
    @Transactional
    public void saveArticleAndMarkParsed(Article article, String articleUrl) {
        articleRepository.save(articleMapper.toEntity(article));
        linkRepository.markParsed(articleUrl);
    }

    @Override
    public List<Article> findAllArticles() {
        return articleMapper.toArticles(articleRepository.findAllOrdered());
    }

    @Override
    public List<ArticleImageRef> findImageRefsForDelivery(int limit) {
        return articleRepository.findImageRefsForDelivery(PageRequest.of(0, limit));
    }
}