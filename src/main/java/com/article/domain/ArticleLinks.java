package com.article.domain;

import com.article.adapter.export.dto.LinkDto;

import java.util.List;

public record ArticleLinks(String articleId, List<LinkDto> topics, List<LinkDto> tags) {}
