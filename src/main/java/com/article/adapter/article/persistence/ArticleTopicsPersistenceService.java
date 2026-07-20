package com.article.adapter.article.persistence;

import com.article.adapter.article.mapper.ArticleMapper;
import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.domain.ArticleRawHtml;
import com.article.domain.ArticleTopicsPage;
import com.article.domain.ArticleMetadataResult;
import com.article.domain.ArticleWithMetadata;
import com.article.domain.TopicTarget;
import com.article.port.persistence.ArticleTopicsPersistencePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleTopicsPersistenceService implements ArticleTopicsPersistencePort {

    private static final String UPDATE_TOPICS_SQL = """
            UPDATE articles
            SET topics = ?::jsonb,
                tags = ?::jsonb,
                main_category = ?,
                topics_extracted = TRUE
            WHERE id = ?
            """;

    private static final String FIND_TOPIC_TARGETS_SQL = """
            SELECT DISTINCT
                   topic.value ->> 'url' AS topic_url,
                   topic.ordinality::integer AS level,
                   a.main_category,
                   (
                       SELECT jsonb_agg(
                                  COALESCE(prefix.value -> 'name', 'null'::jsonb)
                                  ORDER BY prefix.ordinality
                              )
                       FROM jsonb_array_elements(a.topics)
                            WITH ORDINALITY prefix(value, ordinality)
                       WHERE prefix.ordinality <= topic.ordinality
                   ) AS topic_names
            FROM articles a
            CROSS JOIN LATERAL jsonb_array_elements(a.topics)
                 WITH ORDINALITY topic(value, ordinality)
            WHERE a.topics_extracted = TRUE
              AND topic.value ->> 'url' IS NOT NULL
              AND (
                    topic.value ->> 'name' IS NOT NULL
                    OR (
                        topic.ordinality = 1
                        AND jsonb_array_length(a.topics) = 1
                        AND a.main_category IS NOT NULL
                    )
                  )
            ORDER BY topic_url, level, main_category, topic_names
            """;

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<ArticleRawHtml> findPendingBatch(int limit) {
        return articleRepository.findPendingTopicsBatch(limit).stream()
                .map(v -> new ArticleRawHtml(v.getId(), v.getRawHtml()))
                .toList();
    }

    @Override
    @Transactional
    public void saveArticleMetadata(List<ArticleMetadataResult> results) {
        jdbcTemplate.batchUpdate(UPDATE_TOPICS_SQL, results, results.size(), (ps, result) -> {
            ps.setString(1, serializeJson(result.topics()));
            ps.setString(2, serializeJson(result.tags()));
            ps.setString(3, result.mainCategory());
            ps.setString(4, result.articleId());
        });
    }

    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize article metadata as JSON", e);
        }
    }

    @Override
    public List<TopicTarget> findDistinctTopicTargets() {
        return jdbcTemplate.query(FIND_TOPIC_TARGETS_SQL, (rs, rowNum) -> new TopicTarget(
                rs.getString("topic_url"),
                rs.getInt("level"),
                rs.getString("main_category"),
                deserializeTopicNames(rs.getString("topic_names"))
        ));
    }

    @Override
    public ArticleTopicsPage findArticlesByTopic(TopicTarget target, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        boolean standalone = target.level() == 1 && target.topicNames().getFirst() == null;
        List<ArticleEntity> entities = articleRepository.findByTopicTarget(
                target.topicUrl(),
                target.level() - 1,
                target.level(),
                target.mainCategory(),
                serializeJson(target.topicNames()),
                standalone,
                pageable
        );
        List<ArticleWithMetadata> articles = articleMapper.toArticlesWithMetadata(entities);
        return new ArticleTopicsPage(articles);
    }

    private List<String> deserializeTopicNames(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize topic hierarchy from JSON", e);
        }
    }
}
