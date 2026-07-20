package com.article.domain.media;

import lombok.Builder;

import java.time.Instant;

@Builder
public record Video(
        String id,
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
        String connatixMmid,
        String youtubeVideoId
) {}
