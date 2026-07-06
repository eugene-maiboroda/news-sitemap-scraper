package com.article.adapter.video.persistence;

import com.article.adapter.video.mapper.VideoMapper;
import com.article.domain.Video;
import com.article.port.IdGenerationPort;
import com.article.port.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public List<Video> findAllVideos() {
        return videoRepository.findAll().stream()
                .map(videoMapper::toVideo)
                .toList();
    }
}
