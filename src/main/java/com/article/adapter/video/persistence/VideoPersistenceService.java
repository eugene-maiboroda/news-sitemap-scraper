package com.article.adapter.video.persistence;

import com.article.adapter.video.mapper.VideoMapper;
import com.article.domain.media.Video;
import com.article.domain.media.DownloadStatus;
import com.article.domain.media.VideoPlatform;
import com.article.port.out.IdGenerationPort;
import com.article.port.out.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoPersistenceService implements VideoPersistencePort {

    private static final List<VideoPlatform> DOWNLOADABLE_PLATFORMS =
            List.of(VideoPlatform.JW_TOP, VideoPlatform.YOUTUBE);

    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final IdGenerationPort idGenerationPort;

    @Override
    public void save(Video video) {
        videoRepository.save(videoMapper.toEntity(video, idGenerationPort.generateId()));
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
    public List<Video> findPendingVideos(int limit) {
        return videoRepository.findByDownloadStatusAndPlatformInOrderById(
                        DownloadStatus.PENDING,
                        DOWNLOADABLE_PLATFORMS,
                        PageRequest.of(0, limit)
                ).stream()
                .map(videoMapper::toVideo)
                .toList();
    }

    @Override
    @Transactional
    public void markDownloaded(String id, String localFilePath, long fileSize) {
        videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + id))
                .markDownloaded(localFilePath, fileSize);
    }

    @Override
    @Transactional
    public void markFailed(String id) {
        videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + id))
                .markFailed();
    }
}
