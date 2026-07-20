package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "video")
public record VideoProps (
        int timeoutMs,
        String ytDlpCommand
){
}
