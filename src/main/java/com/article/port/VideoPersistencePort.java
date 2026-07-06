package com.article.port;

import com.article.domain.Video;

import java.util.List;

public interface VideoPersistencePort {

    void save(Video video);

    boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId);

    boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid);

    boolean existsByArticleIdAndYoutubeVideoId(String articleId, String youtubeVideoId);

    List<Video> findAllVideos();
}
