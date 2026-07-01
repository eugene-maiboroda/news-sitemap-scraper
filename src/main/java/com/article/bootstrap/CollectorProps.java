package com.article.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "collector")
public record CollectorProps(
        Steps steps,
        int parseBatchSize,
        int parseThreads,
        int imageThreads,
        int deliveryLimit,
        String exportDir,
        String imagesDir,
        List<String> sitemapUrls
) {
    public record Steps(
            boolean discoverLinks,
            boolean parsePages,
            boolean downloadImages,
            boolean exportJson
    ) {}
}
