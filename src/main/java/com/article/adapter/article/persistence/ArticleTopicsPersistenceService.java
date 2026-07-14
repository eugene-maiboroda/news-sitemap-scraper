package com.article.adapter.article.persistence;

import com.article.adapter.article.mapper.ArticleMapper;
import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.adapter.export.dto.LinkDto;
import com.article.domain.Article;
import com.article.domain.ArticleRawHtml;
import com.article.domain.ArticleTopicsPage;
import com.article.domain.ArticleTopicsResult;
import com.article.domain.TopicTarget;
import com.article.port.ArticleTopicsPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleTopicsPersistenceService implements ArticleTopicsPersistencePort {

    private static final String UPDATE_TOPICS_SQL = """
            UPDATE articles SET
                topic1_name = ?, topic1_url = ?,
                topic2_name = ?, topic2_url = ?,
                topic3_name = ?, topic3_url = ?,
                topic4_name = ?, topic4_url = ?,
                topic5_name = ?, topic5_url = ?,
                main_category = ?, topics_extracted = TRUE
            WHERE id = ?
            """;

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ArticleRawHtml> findPendingBatch(int limit) {
        return articleRepository.findPendingTopicsBatch(limit).stream()
                .map(v -> new ArticleRawHtml(v.getId(), v.getRawHtml()))
                .toList();
    }

    @Override
    @Transactional
    public void saveTopicsBatch(List<ArticleTopicsResult> results) {
        jdbcTemplate.batchUpdate(UPDATE_TOPICS_SQL, results, results.size(), (ps, result) -> {
            List<LinkDto> topics = result.topics();
            for (int i = 0; i < 5; i++) {
                LinkDto topic = i < topics.size() ? topics.get(i) : null;
                ps.setString(i * 2 + 1, topic != null ? topic.name() : null);
                ps.setString(i * 2 + 2, topic != null ? topic.url() : null);
            }
            ps.setString(11, result.mainCategory());
            ps.setString(12, result.articleId());
        });
    }

    @Override
    public List<TopicTarget> findDistinctTopicTargets() {
        List<TopicTarget> targets = new ArrayList<>(articleRepository.findDistinctTopicTargetRows().stream()
                .map(row -> new TopicTarget(row.getTopicUrl(), row.getLevel(), row.getMainCategory(),
                        topicNamesUpToLevel(row)))
                .toList());
        // Edge case, additive only: standalone sections with a topic1_url but no topic1_name at
        // all (e.g. "Post Sports+" rendered as a logo, no text) are invisible to the query above.
        targets.addAll(articleRepository.findStandaloneTopicRows().stream()
                .map(row -> new TopicTarget(row.getTopicUrl(), 1, row.getMainCategory(), Arrays.asList((String) null)))
                .toList());
        return targets;
    }

    // Arrays.asList (not List.of!) — topic names can contain null at an intermediate position
    // (e.g. a Post Sports+ badge has no topic1 text), and List.of() rejects null elements.
    private List<String> topicNamesUpToLevel(ArticleRepository.TopicTargetRow row) {
        return switch (row.getLevel()) {
            case 1 -> Arrays.asList(row.getTopic1Name());
            case 2 -> Arrays.asList(row.getTopic1Name(), row.getTopic2Name());
            case 3 -> Arrays.asList(row.getTopic1Name(), row.getTopic2Name(), row.getTopic3Name());
            case 4 -> Arrays.asList(row.getTopic1Name(), row.getTopic2Name(), row.getTopic3Name(), row.getTopic4Name());
            default -> Arrays.asList(row.getTopic1Name(), row.getTopic2Name(), row.getTopic3Name(),
                    row.getTopic4Name(), row.getTopic5Name());
        };
    }

    @Override
    public ArticleTopicsPage findArticlesByTopic(TopicTarget target, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        List<String> t = target.topicNames();
        String url = target.topicUrl();
        String mc = target.mainCategory();
        // level=1 with a null topic1 name can only come from findStandaloneTopicRows() above (the
        // main discovery query requires topic1Name to be non-null for level=1) — route it to the
        // exact-match query, not the generic findByTopic1, which would also pull in articles that
        // have a real deeper topic and already have their own separate target.
        boolean isStandalone = target.level() == 1 && t.get(0) == null;
        List<ArticleEntity> entities = isStandalone
                ? articleRepository.findStandaloneByTopic1(url, mc, pageable)
                : switch (target.level()) {
                    case 1 -> articleRepository.findByTopic1(url, mc, t.get(0), pageable);
                    case 2 -> articleRepository.findByTopic2(url, mc, t.get(0), t.get(1), pageable);
                    case 3 -> articleRepository.findByTopic3(url, mc, t.get(0), t.get(1), t.get(2), pageable);
                    case 4 -> articleRepository.findByTopic4(url, mc, t.get(0), t.get(1), t.get(2), t.get(3), pageable);
                    default -> articleRepository.findByTopic5(url, mc, t.get(0), t.get(1), t.get(2), t.get(3), t.get(4), pageable);
                };

        List<Article> articles = articleMapper.toArticles(entities);
        Map<String, List<LinkDto>> articleTopics = entities.stream()
                .collect(Collectors.toMap(ArticleEntity::getId, this::buildTopicsList));
        return new ArticleTopicsPage(articles, articleTopics);
    }

    private List<LinkDto> buildTopicsList(ArticleEntity e) {
        List<LinkDto> topics = new ArrayList<>();
        addTopicIfPresent(topics, e.getTopic1Name(), e.getTopic1Url());
        addTopicIfPresent(topics, e.getTopic2Name(), e.getTopic2Url());
        addTopicIfPresent(topics, e.getTopic3Name(), e.getTopic3Url());
        addTopicIfPresent(topics, e.getTopic4Name(), e.getTopic4Url());
        addTopicIfPresent(topics, e.getTopic5Name(), e.getTopic5Url());
        return topics;
    }

    private void addTopicIfPresent(List<LinkDto> topics, String name, String url) {
        if (name != null && !name.isBlank()) {
            topics.add(new LinkDto(name, url));
        }
    }
}
