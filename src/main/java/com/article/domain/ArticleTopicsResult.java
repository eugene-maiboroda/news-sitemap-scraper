package com.article.domain;

import com.article.adapter.export.dto.LinkDto;

import java.util.List;

public record ArticleTopicsResult(String articleId, List<LinkDto> topics, String mainCategory) {}
