package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "collector")
public record CollectorProps(
        int parseThreads,
        @DefaultValue("AMERICAN_HUNTER") Site site,
        @DefaultValue("MENU") DiscoverySource discoverySource,
        List<String> sitemapUrls
) {
    public enum DiscoverySource {
        MENU,
        SITEMAP
    }

    public enum Site {
        AMERICAN_HUNTER,
        AMERICAN_RIFLEMAN
    }
}
