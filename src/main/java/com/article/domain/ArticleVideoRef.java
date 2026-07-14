package com.article.domain;

import com.article.domain.status.VideoPlatform;

public record ArticleVideoRef(
        String id,
        String articleId,
        String category,
        VideoPlatform platform,
        String m3u8Url,
        String youtubeVideoId
) {}
