package com.article.adapter.export.resolver;

import com.article.adapter.export.model.JsonExportTarget;
import com.article.adapter.export.model.PreparedArticleExport;

import java.util.List;

public interface ExportTargetResolver {

    List<JsonExportTarget> resolve(PreparedArticleExport article);

    default List<JsonExportTarget> initialTargets() {
        return List.of();
    }
}
