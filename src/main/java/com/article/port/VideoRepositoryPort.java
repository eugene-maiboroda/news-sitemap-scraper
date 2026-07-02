package com.article.port;

import com.article.domain.Video;

public interface VideoRepositoryPort {

    void save(Video video);

    boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId);

    boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid);
}
