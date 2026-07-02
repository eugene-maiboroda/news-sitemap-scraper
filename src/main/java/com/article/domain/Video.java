package com.article.domain;

import lombok.Builder;

import java.time.Instant;

@Builder
public record Video(
        String articleId,
        VideoPlatform platform,
        String jwMediaId,
        String m3u8Url,
        DownloadStatus downloadStatus,
        String localFilePath,
        Long fileSize,
        String title,
        String description,
        String thumbnailUrl,
        Integer durationSeconds,
        Instant publishedAt,
        String connatixContentId,
        String connatixStreamId,
        String connatixMmid
) {}
