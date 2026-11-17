package com.article.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "steps")
public record StepsProps(
        Step discoverLinks,
        ParsePagesStep parsePages,
        AuthorStep extractAuthors,
        Step downloadImages,
        Step downloadVideos,
        Step exportJson
) {

    public enum ParseSource {

        HTTP,

        RAW_HTML
    }

    public record ParsePagesStep(

            boolean enabled,

            int batchSize,

            @DefaultValue("HTTP") ParseSource source
    ) {
    }

    public record AuthorStep(

            boolean enabled
    ) {}

    public record Step(
            boolean enabled,
            int batchSize

    ) {
    }
}
