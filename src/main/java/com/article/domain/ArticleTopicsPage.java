package com.article.domain;

import com.article.adapter.export.dto.LinkDto;

import java.util.List;
import java.util.Map;

/**
 * One page of articles matching a {@link TopicTarget}, plus their per-article topics keyed by
 * article id. Both parts are built from the same already-fetched entity rows in
 * ArticleTopicsPersistenceService — no second database round-trip.
 */
public record ArticleTopicsPage(List<Article> articles, Map<String, List<LinkDto>> articleTopics) {}
