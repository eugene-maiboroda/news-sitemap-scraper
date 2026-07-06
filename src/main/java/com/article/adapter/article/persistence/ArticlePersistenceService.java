package com.article.adapter.article.persistence;

import com.article.adapter.article.mapper.ArticleMapper;
import com.article.domain.Article;
import com.article.domain.ArticleImageRef;
import com.article.domain.ArticleLink;
import com.article.domain.status.CrawlStatus;
import com.article.port.ArticleRepositoryPort;
import com.article.port.IdGenerationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticlePersistenceService implements ArticleRepositoryPort {

    private static final String BULK_INSERT_SQL = """
            INSERT INTO article_links (id, article_url, article_date, status)
            VALUES (?, ?, ?, 'NEW')
            ON CONFLICT (article_url) DO NOTHING
            """;

    private final ArticleLinkRepository linkRepository;
    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final JdbcTemplate jdbcTemplate;
    private final IdGenerationPort idGenerationPort;

    @Override
    @Transactional
    public void insertLinks(List<ArticleLink> links) {
        long start = System.currentTimeMillis();
        jdbcTemplate.batchUpdate(BULK_INSERT_SQL, links, 1000, (ps, link) -> {
            ps.setString(1, idGenerationPort.generateId());
            ps.setString(2, link.articleUrl());
            ps.setObject(3, link.articleDate());
        });
        log.info("Insert took {} ms", System.currentTimeMillis() - start);
    }

    @Override
    public List<ArticleLink> findNewBatch(int limit) {
        return articleMapper.toArticleLinks(
                linkRepository.findByStatusOrderByArticleDate(CrawlStatus.NEW, PageRequest.of(0, limit)));
    }

    @Override
    @Transactional
    public void saveArticleAndMarkParsed(Article article, String articleUrl) {
        articleRepository.save(articleMapper.toEntity(article));
        linkRepository.updateStatus(articleUrl, CrawlStatus.PARSED);
    }

    @Override
    @Transactional
    public void markError(String articleUrl, String errorMessage) {
        linkRepository.updateStatusWithError(articleUrl, CrawlStatus.ERROR, errorMessage);
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
