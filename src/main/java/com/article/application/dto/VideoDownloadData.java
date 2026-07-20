package com.article.application.dto;

import com.article.domain.media.VideoPlatform;

public record VideoDownloadData(
        String id,
        String articleId,
        String category,
        VideoPlatform platform,
        String m3u8Url,
        String youtubeVideoId
) {}
