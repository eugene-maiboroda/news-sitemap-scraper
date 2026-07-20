package com.article.adapter.article.persistence;

import com.article.adapter.article.mapper.ArticleMapper;
import com.article.domain.ArticleLinkValue;
import com.article.domain.ArticleMetadataResult;
import com.article.domain.TopicTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleTopicsPersistenceServiceTest {

    @Test
    void serializesCompleteMetadataAndBindsFourUpdateParameters() throws Exception {
        ArticleRepository repository = mock(ArticleRepository.class);
        ArticleMapper mapper = mock(ArticleMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ArticleTopicsPersistenceService service =
                new ArticleTopicsPersistenceService(repository, mapper, jdbcTemplate, objectMapper);

        List<ArticleLinkValue> topics = List.of(
                new ArticleLinkValue("Topic 1", "https://example.com/1"),
                new ArticleLinkValue("Topic 2", "https://example.com/2"),
                new ArticleLinkValue("Topic 3", "https://example.com/3"),
                new ArticleLinkValue("Topic 4", "https://example.com/4"),
                new ArticleLinkValue("Topic 5", "https://example.com/5"),
                new ArticleLinkValue(null, "https://example.com/6")
        );
        List<ArticleLinkValue> tags = List.of(
                new ArticleLinkValue("Tag", null)
        );
        ArticleMetadataResult result =
                new ArticleMetadataResult("article-1", topics, tags, "Sports");

        service.saveArticleMetadata(List.of(result));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ParameterizedPreparedStatementSetter<ArticleMetadataResult>> setterCaptor =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(
                anyString(), eq(List.of(result)), eq(1), setterCaptor.capture());

        PreparedStatement statement = mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(statement, result);

        verify(statement).setString(1, objectMapper.writeValueAsString(topics));
        verify(statement).setString(2, objectMapper.writeValueAsString(tags));
        verify(statement).setString(3, "Sports");
        verify(statement).setString(4, "article-1");
    }

    @Test
    void discoversTopicTargetWithNullNameInsideHierarchy() throws Exception {
        ArticleRepository repository = mock(ArticleRepository.class);
        ArticleMapper mapper = mock(ArticleMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ArticleTopicsPersistenceService service =
                new ArticleTopicsPersistenceService(repository, mapper, jdbcTemplate, new ObjectMapper());
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("topic_url")).thenReturn("https://example.com/knicks");
        when(resultSet.getInt("level")).thenReturn(2);
        when(resultSet.getString("main_category")).thenReturn("Post Sports+");
        when(resultSet.getString("topic_names")).thenReturn("[null,\"Inside the Knicks\"]");

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RowMapper<TopicTarget> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(resultSet, 0));
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class));

        List<TopicTarget> targets = service.findDistinctTopicTargets();

        assertEquals(1, targets.size());
        assertEquals(Arrays.asList(null, "Inside the Knicks"), targets.getFirst().topicNames());
    }

    @Test
    void bindsAStandaloneJsonbTargetToTheSingleMembershipQuery() {
        ArticleRepository repository = mock(ArticleRepository.class);
        ArticleMapper mapper = mock(ArticleMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ArticleTopicsPersistenceService service =
                new ArticleTopicsPersistenceService(repository, mapper, jdbcTemplate, new ObjectMapper());
        TopicTarget target = new TopicTarget(
                "https://example.com/sports-plus",
                1,
                "Post Sports+",
                Arrays.asList((String) null)
        );
        when(repository.findByTopicTarget(
                anyString(), anyInt(), anyInt(), anyString(), anyString(), anyBoolean(), any(Pageable.class)))
                .thenReturn(List.of());

        service.findArticlesByTopic(target, 0, 500);

        verify(repository).findByTopicTarget(
                "https://example.com/sports-plus",
                0,
                1,
                "Post Sports+",
                "[null]",
                true,
                Pageable.ofSize(500)
        );
    }
}
