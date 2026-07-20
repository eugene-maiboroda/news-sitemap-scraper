package com.article.adapter.export.model;

import java.nio.file.Path;
import java.util.List;

public record JsonExportTarget(
        Path relativePath,
        String topicUrl,
        List<String> hierarchy
) {
    public static JsonExportTarget category(String fileName) {
        return new JsonExportTarget(Path.of(fileName), null, List.of());
    }

    public static JsonExportTarget topic(
            Path relativePath,
            String topicUrl,
            List<String> hierarchy
    ) {
        return new JsonExportTarget(relativePath, topicUrl, List.copyOf(hierarchy));
    }

    public boolean isTopic() {
        return topicUrl != null;
    }
}
