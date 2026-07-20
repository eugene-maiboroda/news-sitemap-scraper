package com.article.application;

import com.article.domain.Image;
import com.article.domain.Video;
import com.article.port.persistence.ImagePersistencePort;
import com.article.port.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParsedMediaService {

    private final VideoPersistencePort videoPersistencePort;
    private final ImagePersistencePort imagePersistencePort;

    public void saveVideos(List<Video> videos) {
        for (Video video : videos) {
            boolean exists = switch (video.getPlatform()) {
                case JW_TOP ->
                        videoPersistencePort.existsByArticleIdAndJwMediaId(video.getArticleId(), video.getJwMediaId());
                case CONNATIX_BOTTOM ->
                        videoPersistencePort.existsByArticleIdAndConnatixMmid(video.getArticleId(), video.getConnatixMmid());
                case YOUTUBE ->
                        videoPersistencePort.existsByArticleIdAndYoutubeVideoId(video.getArticleId(), video.getYoutubeVideoId());
            };
            if (exists) {
                log.debug("[VIDEO][SKIP] duplicate platform={} articleId={}", video.getPlatform(), video.getArticleId());
                continue;
            }
            videoPersistencePort.save(video);
            log.info("[VIDEO][SAVED] platform={} articleId={}", video.getPlatform(), video.getArticleId());
        }
    }

    public void saveImages(List<Image> images) {
        for (Image image : images) {
            if (imagePersistencePort.existsByArticleIdAndImageUrl(image.getArticleId(), image.getImageUrl())) {
                log.debug("[IMAGE][SKIP] duplicate articleId={} imageUrl={}", image.getArticleId(), image.getImageUrl());
                continue;
            }
            imagePersistencePort.save(image);
            log.info("[IMAGE][SAVED] articleId={} position={}", image.getArticleId(), image.getPosition());
        }
    }
}
