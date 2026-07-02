package com.article.domain;

import lombok.Builder;

@Builder
public record Image(
        String articleId,
        String imageUrl,
        String caption,
        int position,
        DownloadStatus downloadStatus,
        String localFilePath,
        Long fileSize
) {}
