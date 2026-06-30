package com.article.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collector")
public record CollectorProps(
        Steps steps,
        int parseBatchSize,
        int parseThreads,
        int imageThreads,
        int deliveryLimit,
        String exportDir,
        String imagesDir
) {
    public record Steps(
            boolean collectLinks,
            boolean parsePages,
            boolean downloadImages,
            boolean exportJson
    ) {}
}
