package com.article.domain;

import java.util.List;

/**
 * One distinct topic-hierarchy export target. The same topicUrl can legitimately have different
 * hierarchies across articles (e.g. a team page reached from the regular section, Post Sports+,
 * and Sports Betting) — {@code level}/{@code mainCategory}/{@code topicNames} together are the
 * true identity of a target, used identically for discovery grouping, exact article membership
 * matching, and filename hashing. {@code hierarchy} for display is deliberately NOT stored here —
 * it's a lossy (deduplicated) projection of these fields, computed on demand via
 * {@code ParseUtils.buildHierarchy(mainCategory, topicNames)}.
 */
public record TopicTarget(
        String topicUrl,
        int level,
        String mainCategory,
        List<String> topicNames
) {}
