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
        ExportJsonStep exportJson
) {

    public enum ParseSource {

        HTTP,

        RAW_HTML
    }

    public enum ArchiveOperation {

        CAPTIONS,

        MEDIA_CONTENT
    }

    public record ParsePagesStep(

            boolean enabled,

            int batchSize,

            @DefaultValue("HTTP") ParseSource source,

            @DefaultValue("CAPTIONS") ArchiveOperation archiveOperation
    ) {
    }

    public record AuthorStep(

            boolean enabled
    ) {}

    public record ExportJsonStep(

            boolean enabled,

            int batchSize,

            String fileName
    ) {}

    public record Step(
            boolean enabled,
            int batchSize

    ) {
    }
}
