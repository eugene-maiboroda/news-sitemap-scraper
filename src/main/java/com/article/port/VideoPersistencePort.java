package com.article.port;

import com.article.domain.ArticleVideoRef;
import com.article.domain.CategoryPendingCount;
import com.article.domain.Video;

import java.util.List;

public interface VideoPersistencePort {

    void save(Video video);

    boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId);

    boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid);

    boolean existsByArticleIdAndYoutubeVideoId(String articleId, String youtubeVideoId);

    List<Video> findVideosByArticleIds(List<String> articleIds);

    List<CategoryPendingCount> findCategoriesOrderedByPendingCount();

    List<ArticleVideoRef> findPendingVideosForCategory(String category, int limit);

    List<ArticleVideoRef> findPendingVideosWithoutCategory(int limit);

    void markDownloaded(String id, String localFilePath, long fileSize);

    void markFailed(String id);
}
