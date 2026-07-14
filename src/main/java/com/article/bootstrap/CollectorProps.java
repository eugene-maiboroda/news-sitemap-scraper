package com.article.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "collector")
public record CollectorProps(
        Steps steps,
        Export export,
        int parseBatchSize,
        int parseThreads,
        int imageThreads,
        int imageBatchSize,
        String exportDir,
        String imagesDir,
        String videosDir,
        int videoBatchSize,
        int videoDownloadTimeoutSeconds,
        String ytDlpCommand,
        List<String> sitemapUrls
) {
    public record Steps(
            boolean discoverLinks,
            boolean parsePages,
            boolean extractTopics,
            boolean downloadImages,
            boolean downloadVideos,
            boolean exportJson,
            boolean extractConnatixVideos
    ) {}

    public record Export(String mode) {}
}
