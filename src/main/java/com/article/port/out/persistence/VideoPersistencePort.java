package com.article.port.out.persistence;

import com.article.domain.media.Video;

import java.util.List;

public interface VideoPersistencePort {

    void save(Video video);

    boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId);

    boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid);

    boolean existsByArticleIdAndYoutubeVideoId(String articleId, String youtubeVideoId);

    List<Video> findVideosByArticleIds(List<String> articleIds);

    List<Video> findPendingVideos(int limit);

    void markDownloaded(String id, String localFilePath, long fileSize);

    void markFailed(String id);
}
