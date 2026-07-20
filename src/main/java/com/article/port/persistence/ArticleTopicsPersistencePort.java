package com.article.port.persistence;

import com.article.domain.ArticleRawHtml;
import com.article.domain.ArticleTopicsPage;
import com.article.domain.ArticleMetadataResult;
import com.article.domain.TopicTarget;

import java.util.List;

public interface ArticleTopicsPersistencePort {

    List<ArticleRawHtml> findPendingBatch(int limit);

    void saveArticleMetadata(List<ArticleMetadataResult> results);

    List<TopicTarget> findDistinctTopicTargets();

    ArticleTopicsPage findArticlesByTopic(TopicTarget target, int page, int pageSize);
}
