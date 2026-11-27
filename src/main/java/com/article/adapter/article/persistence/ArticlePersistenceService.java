package com.article.adapter.article.persistence;

import com.article.adapter.article.mapper.ArticleMapper;
import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.adapter.article.persistence.entity.CrawlStatus;
import com.article.adapter.article.persistence.entity.LinkArticleEntity;
import com.article.application.PipelineRunSettings;
import com.article.domain.article.Article;
import com.article.domain.article.ArticleLink;
import com.article.domain.article.ArticleMetadata;
import com.article.domain.article.ArticleWithMetadata;
import com.article.port.out.IdGenerationPort;
import com.article.port.out.persistence.ArticlePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticlePersistenceService implements ArticlePersistencePort {

    private static final String BULK_INSERT_SQL = """
            INSERT INTO article_links (id, article_url, article_date, status, collection_batch_id)
            VALUES (?, ?, ?, 'NEW', ?)
            ON CONFLICT (article_url) DO NOTHING
            """;

    private final ArticleLinkRepository linkRepository;
    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final JdbcTemplate jdbcTemplate;
    private final IdGenerationPort idGenerationPort;
    private final PipelineRunSettings runSettings;

    @Override
    @Transactional
    public void insertLinks(List<ArticleLink> links) {
        if (links.isEmpty()) {
            return;
        }
        String collectionBatchId = runSettings.current().collector().collectionBatchId();
        if (collectionBatchId == null || collectionBatchId.isBlank()) {
            throw new IllegalArgumentException("Set collector.collection-batch-id before discovering new links");
        }

        long start = System.currentTimeMillis();
        jdbcTemplate.batchUpdate(BULK_INSERT_SQL, links, links.size(), (ps, link) -> {
            ps.setString(1, idGenerationPort.generateId());
            ps.setString(2, link.articleUrl());
            ps.setObject(3, link.articleDate());
            ps.setString(4, collectionBatchId);
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
    public void saveArticleAndMarkParsed(Article article, ArticleMetadata metadata, String articleUrl) {
        articleRepository.save(articleMapper.toEntity(article, metadata));
        findLink(articleUrl).markParsed();
    }

    @Override
    @Transactional
    public void markError(String articleUrl, String errorMessage) {
        findLink(articleUrl).markFailed(errorMessage);
    }

    @Override
    public List<ArticleWithMetadata> findArticlesForExport(int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        String collectionBatchId = runSettings.current().export().collectionBatchId();
        List<ArticleEntity> articles;
        if (collectionBatchId == null) {
            articles = articleRepository.findAllByOrderByPublishDateAscIdAsc(pageRequest);
        } else {
            articles = articleRepository.findByCollectionBatchId(collectionBatchId, pageRequest);
        }
        return articleMapper.toArticlesWithMetadata(articles);
    }

    @Override
    public Map<String, String> findCategoriesByArticleIds(List<String> articleIds) {
        Map<String, String> categories = new HashMap<>();
        articleRepository.findAllById(articleIds)
                .forEach(article -> categories.put(article.getId(), article.getCategory()));
        return categories;
    }

    private LinkArticleEntity findLink(String articleUrl) {
        return linkRepository.findByArticleUrl(articleUrl)
                .orElseThrow(() -> new IllegalArgumentException("Article link not found: " + articleUrl));
    }
}
