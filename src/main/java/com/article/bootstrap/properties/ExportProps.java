package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "export")
public record ExportProps(ExportMode mode) {

    public ExportProps {
        mode = mode == null ? ExportMode.CATEGORY : mode;
    }
}
