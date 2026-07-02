package com.article.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "browser")
public record BrowserProps(boolean headless, int timeoutMs) {}
