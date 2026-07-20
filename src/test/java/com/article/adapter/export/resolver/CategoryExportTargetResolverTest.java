package com.article.adapter.export.resolver;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.domain.article.ArticleMetadata;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryExportTargetResolverTest {

    @Test
    void routesBlankCategoryToUncategorizedFile() {
        PreparedArticleExport article = new PreparedArticleExport(
                ArticleExportDto.builder().id("a1").category(null).build(),
                new ArticleMetadata(List.of(), List.of(), null)
        );

        assertEquals(
                Path.of("uncategorized.json"),
                new CategoryExportTargetResolver().resolve(article).getFirst().relativePath()
        );
    }
}
