package com.article.application;

import com.article.domain.media.Image;
import com.article.domain.media.Video;
import com.article.port.out.persistence.ImagePersistencePort;
import com.article.port.out.persistence.VideoPersistencePort;
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
            boolean exists = switch (video.platform()) {
                case JW_TOP ->
                        videoPersistencePort.existsByArticleIdAndJwMediaId(video.articleId(), video.jwMediaId());
                case CONNATIX_BOTTOM ->
                        videoPersistencePort.existsByArticleIdAndConnatixMmid(video.articleId(), video.connatixMmid());
                case YOUTUBE ->
                        videoPersistencePort.existsByArticleIdAndYoutubeVideoId(video.articleId(), video.youtubeVideoId());
            };
            if (exists) {
                log.debug("[VIDEO][SKIP] duplicate platform={} articleId={}", video.platform(), video.articleId());
                continue;
            }
            videoPersistencePort.save(video);
            log.info("[VIDEO][SAVED] platform={} articleId={}", video.platform(), video.articleId());
        }
    }

    public void saveImages(List<Image> images) {
        imagePersistencePort.saveAll(images);
        log.info("[IMAGE][SAVED] count={}", images.size());
    }
}
