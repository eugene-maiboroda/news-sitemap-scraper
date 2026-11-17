package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "steps")
public record StepsProps(
        Step discoverLinks,
        Step parsePages,
        AuthorStep extractAuthors,
        Step downloadImages,
        Step downloadVideos,
        Step exportJson,
        Step extractConnatixVideos
) {

    public record AuthorStep(

            boolean enabled
    ) {}

    public record Step(
            boolean enabled,
            int batchSize

    ) {
    }
}