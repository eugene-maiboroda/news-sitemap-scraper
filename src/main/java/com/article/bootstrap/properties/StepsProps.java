package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "steps")
public record StepsProps(
        Step discoverLinks,
        Step parsePages,
        Step downloadImages,
        Step downloadVideos,
        Step processAuthors,
        ToggleStep exportAuthors,
        Step exportJson,
        ToggleStep extractConnatixVideos
) {

    public record Step(
            boolean enabled,
            int batchSize

    ) {
    }

    public record ToggleStep(
            boolean enabled
    ) {
    }
}
