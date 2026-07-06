package com.article.application;

import com.article.domain.Image;
import com.article.domain.Video;
import com.article.port.ImageRepositoryPort;
import com.article.port.VideoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParsedMediaService {

    private final VideoRepositoryPort videoRepositoryPort;
    private final ImageRepositoryPort imageRepositoryPort;

    public void saveVideos(List<Video> videos) {
        for (Video video : videos) {
            boolean exists = switch (video.getPlatform()) {
                case JW_TOP ->
                        videoRepositoryPort.existsByArticleIdAndJwMediaId(video.getArticleId(), video.getJwMediaId());
                case CONNATIX_BOTTOM ->
                        videoRepositoryPort.existsByArticleIdAndConnatixMmid(video.getArticleId(), video.getConnatixMmid());
                case YOUTUBE ->
                        videoRepositoryPort.existsByArticleIdAndYoutubeVideoId(video.getArticleId(), video.getYoutubeVideoId());
            };
            if (exists) {
                log.debug("[VIDEO][SKIP] duplicate platform={} articleId={}", video.getPlatform(), video.getArticleId());
                continue;
            }
            videoRepositoryPort.save(video);
            log.info("[VIDEO][SAVED] platform={} articleId={}", video.getPlatform(), video.getArticleId());
        }
    }

    public void saveImages(List<Image> images) {
        for (Image image : images) {
            if (imageRepositoryPort.existsByArticleIdAndImageUrl(image.getArticleId(), image.getImageUrl())) {
                log.debug("[IMAGE][SKIP] duplicate articleId={} imageUrl={}", image.getArticleId(), image.getImageUrl());
                continue;
            }
            imageRepositoryPort.save(image);
            log.info("[IMAGE][SAVED] articleId={} position={}", image.getArticleId(), image.getPosition());
        }
    }
}
