package com.article.application.dto;

import com.article.adapter.export.dto.LinkDto;
import com.article.domain.TopicTarget;

import java.util.List;
import java.util.Map;

public record TopicExportContext(
        TopicTarget target,
        Map<String, List<LinkDto>> articleTopics
) {}
