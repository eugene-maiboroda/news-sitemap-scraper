package com.article.adapter.export.model;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.domain.article.ArticleMetadata;

public record PreparedArticleExport(
        ArticleExportDto article,
        ArticleMetadata metadata
) {
}
