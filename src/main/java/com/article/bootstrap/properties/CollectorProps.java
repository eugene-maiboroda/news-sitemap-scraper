package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "collector")
public record CollectorProps(

        int parseThreads,

        long sitemapDelayMs,

        List<String> sitemapUrls,

        String collectionBatchId
) {

    public CollectorProps {
        collectionBatchId = collectionBatchId == null || collectionBatchId.isBlank()
                ? null : collectionBatchId.trim();
    }
}
