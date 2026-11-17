package com.article.adapter.article.persistence;

import com.article.adapter.article.mapper.ArticleMapper;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.domain.Author;
import com.article.domain.DiscoveredArticle;
import com.article.domain.status.CrawlStatus;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.IdGenerationPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticlePersistenceService implements ArticlePersistencePort {

    private static final int DISCOVERY_BATCH_SIZE = 500;

    private static final String BULK_INSERT_SQL = """
            INSERT INTO article_links (id, article_url, article_date, status)
            VALUES (?, ?, ?, 'NEW')
            ON CONFLICT (article_url) DO NOTHING
            """;

    private static final String UPSERT_DISCOVERED_ARTICLE_SQL = """
            INSERT INTO article_links (
                id, article_url, status, discovery_title, discovery_description, menu_paths
            )
            VALUES (?, ?, 'NEW', ?, ?, CAST(? AS jsonb))
            ON CONFLICT (article_url) DO UPDATE SET
                discovery_title = COALESCE(NULLIF(article_links.discovery_title, ''), EXCLUDED.discovery_title),
                discovery_description = COALESCE(
                    NULLIF(article_links.discovery_description, ''), EXCLUDED.discovery_description
                ),
                menu_paths = (
                    SELECT COALESCE(jsonb_object_agg(paths.section_url, paths.hierarchies), '{}'::jsonb)
                    FROM (
                        SELECT section_url, jsonb_agg(DISTINCT hierarchy ORDER BY hierarchy) AS hierarchies
                        FROM (
                            SELECT key AS section_url, value AS section_paths
                            FROM jsonb_each(COALESCE(article_links.menu_paths, '{}'::jsonb))
                            UNION ALL
                            SELECT key, value FROM jsonb_each(EXCLUDED.menu_paths)
                        ) AS sections
                        CROSS JOIN LATERAL jsonb_array_elements(section_paths) AS entries(hierarchy)
                        GROUP BY section_url
                    ) AS paths
                )
            """;

    private final ArticleLinkRepository linkRepository;
    private final ObjectProvider<ArticleRepository> articleRepositoryProvider;
    private final ArticleMapper articleMapper;
    private final JdbcTemplate jdbcTemplate;
    private final IdGenerationPort idGenerationPort;

    private final ObjectMapper objectMapper;

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
    @Transactional
    public void saveDiscoveredArticles(List<DiscoveredArticle> articles) {
        List<DiscoveredArticle> articlesByUrl = articles.stream()
                .sorted(Comparator.comparing(DiscoveredArticle::articleUrl))
                .toList();

        jdbcTemplate.batchUpdate(UPSERT_DISCOVERED_ARTICLE_SQL, articlesByUrl, DISCOVERY_BATCH_SIZE, (statement, article) -> {
            statement.setString(1, idGenerationPort.generateId());
            statement.setString(2, article.articleUrl());
            statement.setString(3, article.title());
            statement.setString(4, article.description());
            statement.setString(5, serializeMenuPaths(article));
        });
    }

    private String serializeMenuPaths(DiscoveredArticle article) {
        try {
            return objectMapper.writeValueAsString(article.menuPaths());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid menu paths for " + article.articleUrl(), e);
        }
    }

    @Override
    public List<ArticleLink> findNewBatch(int limit) {
        return articleMapper.toArticleLinks(
                linkRepository.findByStatusOrderByArticleDate(CrawlStatus.NEW, PageRequest.of(0, limit)));
    }

    @Override
    @Transactional
    public void saveArticleAndMarkParsed(Article article, String articleUrl) {
        articleRepositoryProvider.getObject().save(articleMapper.toEntity(article));
        linkRepository.updateStatus(articleUrl, CrawlStatus.PARSED);
    }

    @Override
    @Transactional
    public void markError(String articleUrl, String errorMessage) {
        linkRepository.updateStatusWithError(articleUrl, CrawlStatus.ERROR, errorMessage);
    }

    @Override
    public List<String> findDistinctCategories() {
        return articleRepositoryProvider.getObject().findDistinctCategories();
    }

    @Override
    public List<Article> findArticlesByCategory(String category, int page, int pageSize) {
        return articleMapper.toArticles(
                articleRepositoryProvider.getObject().findByCategoryOrderById(category, PageRequest.of(page, pageSize)));
    }

    @Override
    public List<Article> findArticlesWithoutCategory(int page, int pageSize) {
        return articleMapper.toArticles(
                articleRepositoryProvider.getObject().findArticlesWithoutCategory(PageRequest.of(page, pageSize)));
    }

    @Override
    public List<Article> findAllArticlesOrdered(int page, int pageSize) {
        return articleMapper.toArticles(
                articleRepositoryProvider.getObject().findAllByOrderById(PageRequest.of(page, pageSize)));
    }

    @Override
    public List<Author> findDistinctAuthors() {
        return articleRepositoryProvider.getObject().findDistinctAuthors().stream()
                .map(row -> new Author(
                        idGenerationPort.generateId(),
                        row.getAuthorName(),
                        row.getAuthorUrl(),
                        null,
                        null,
                        null
                ))
                .toList();
    }
}

