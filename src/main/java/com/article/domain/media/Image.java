package com.article.domain.media;

import lombok.Builder;

@Builder
public record Image(
        String id,
        String articleId,
        String imageUrl,
        String caption,
        int position,
        DownloadStatus downloadStatus,
        String localFilePath,
        Long fileSize
) {}
