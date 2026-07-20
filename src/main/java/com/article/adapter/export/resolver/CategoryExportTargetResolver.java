package com.article.adapter.export.resolver;

import com.article.adapter.export.model.JsonExportTarget;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.adapter.shared.FileNaming;

import java.util.List;

public class CategoryExportTargetResolver implements ExportTargetResolver {

    @Override
    public List<JsonExportTarget> resolve(PreparedArticleExport article) {
        String category = FileNaming.categorySlug(article.article().category());
        return List.of(JsonExportTarget.category(category + ".json"));
    }
}
