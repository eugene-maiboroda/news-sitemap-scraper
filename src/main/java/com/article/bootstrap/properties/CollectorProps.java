package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "collector")
public record CollectorProps(
        int parseThreads,
        List<String> sitemapUrls,
        @DefaultValue("SITEMAP") DiscoverySource discoverySource
) {
    public enum DiscoverySource {
        SITEMAP,
        AMERICAN_HUNTER
    }
}
