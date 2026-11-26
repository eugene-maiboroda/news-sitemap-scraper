package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "export")
public record ExportProps(

        ExportMode mode,

        String collectionBatchId
) {

    public ExportProps {
        mode = mode == null ? ExportMode.CATEGORY : mode;
        collectionBatchId = collectionBatchId == null || collectionBatchId.isBlank()
                ? null : collectionBatchId.trim();
    }
}
