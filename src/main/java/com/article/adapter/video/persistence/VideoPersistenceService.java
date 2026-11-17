package com.article.adapter.video.persistence;

import com.article.adapter.video.mapper.VideoMapper;
import com.article.domain.ArticleVideoRef;
import com.article.domain.CategoryPendingCount;
import com.article.domain.Video;
import com.article.port.IdGenerationPort;
import com.article.port.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoPersistenceService implements VideoPersistencePort {

    private final ObjectProvider<VideoRepository> videoRepositoryProvider;
    private final VideoMapper videoMapper;
    private final IdGenerationPort idGenerationPort;

    @Override
    public void save(Video video) {
        video.setId(idGenerationPort.generateId());
        videoRepositoryProvider.getObject().save(videoMapper.toEntity(video));
    }

    @Override
    public boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId) {
        return videoRepositoryProvider.getObject().existsByArticleIdAndJwMediaId(articleId, jwMediaId);
    }

    @Override
    public boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid) {
        return videoRepositoryProvider.getObject().existsByArticleIdAndConnatixMmid(articleId, connatixMmid);
    }

    @Override
    public boolean existsByArticleIdAndYoutubeVideoId(String articleId, String youtubeVideoId) {
        return videoRepositoryProvider.getObject().existsByArticleIdAndYoutubeVideoId(articleId, youtubeVideoId);
    }

    @Override
    public List<Video> findVideosByArticleIds(List<String> articleIds) {
        return videoRepositoryProvider.getObject().findByArticleIdIn(articleIds).stream()
                .map(videoMapper::toVideo)
                .toList();
    }

    @Override
    public List<CategoryPendingCount> findCategoriesOrderedByPendingCount() {
        return videoRepositoryProvider.getObject().findCategoriesOrderedByPendingCount();
    }

    @Override
    public List<ArticleVideoRef> findPendingVideosForCategory(String category, int limit) {
        return videoRepositoryProvider.getObject().findPendingVideosForCategory(category, PageRequest.of(0, limit));
    }

    @Override
    public List<ArticleVideoRef> findPendingVideosWithoutCategory(int limit) {
        return videoRepositoryProvider.getObject().findPendingVideosWithoutCategory(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markDownloaded(String id, String localFilePath, long fileSize) {
        videoRepositoryProvider.getObject().markDownloaded(id, localFilePath, fileSize);
    }

    @Override
    @Transactional
    public void markFailed(String id) {
        videoRepositoryProvider.getObject().markFailed(id);
    }
}
