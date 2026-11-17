package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProps(
        String exportDir,
        String imagesDir,
        String videosDir
) {
}
