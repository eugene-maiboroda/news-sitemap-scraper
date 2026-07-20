package com.article.adapter.video.persistence;

import com.article.adapter.video.mapper.VideoMapper;
import com.article.domain.ArticleVideoRef;
import com.article.domain.CategoryPendingCount;
import com.article.domain.Video;
import com.article.port.IdGenerationPort;
import com.article.port.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoPersistenceService implements VideoPersistencePort {

    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final IdGenerationPort idGenerationPort;

    @Override
    public void save(Video video) {
        video.setId(idGenerationPort.generateId());
        videoRepository.save(videoMapper.toEntity(video));
    }

    @Override
    public boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId) {
        return videoRepository.existsByArticleIdAndJwMediaId(articleId, jwMediaId);
    }

    @Override
    public boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid) {
        return videoRepository.existsByArticleIdAndConnatixMmid(articleId, connatixMmid);
    }

    @Override
    public boolean existsByArticleIdAndYoutubeVideoId(String articleId, String youtubeVideoId) {
        return videoRepository.existsByArticleIdAndYoutubeVideoId(articleId, youtubeVideoId);
    }

    @Override
    public List<Video> findVideosByArticleIds(List<String> articleIds) {
        return videoRepository.findByArticleIdIn(articleIds).stream()
                .map(videoMapper::toVideo)
                .toList();
    }

    @Override
    public List<CategoryPendingCount> findCategoriesOrderedByPendingCount() {
        return videoRepository.findCategoriesOrderedByPendingCount();
    }

    @Override
    public List<ArticleVideoRef> findPendingVideosForCategory(String category, int limit) {
        return videoRepository.findPendingVideosForCategory(category, PageRequest.of(0, limit));
    }

    @Override
    public List<ArticleVideoRef> findPendingVideosWithoutCategory(int limit) {
        return videoRepository.findPendingVideosWithoutCategory(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markDownloaded(String id, String localFilePath, long fileSize) {
        videoRepository.markDownloaded(id, localFilePath, fileSize);
    }

    @Override
    @Transactional
    public void markFailed(String id) {
        videoRepository.markFailed(id);
    }
}
