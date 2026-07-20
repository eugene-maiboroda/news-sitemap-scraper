package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "browser")
public record BrowserProps(
        boolean enabled,
        boolean headless,
        int timeoutMs

) {}