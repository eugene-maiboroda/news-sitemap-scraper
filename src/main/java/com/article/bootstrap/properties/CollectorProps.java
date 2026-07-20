package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "collector")
public record CollectorProps(
        int parseThreads,
        List<String> sitemapUrls
) {
}
