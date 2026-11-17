package com.article.domain;

public record ArticleVideoRef(

        String id,

        String articleId,

        String sourceUrl,

        String youtubeVideoId
) {}
