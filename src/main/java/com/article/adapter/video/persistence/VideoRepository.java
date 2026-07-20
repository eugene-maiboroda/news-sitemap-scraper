package com.article.adapter.video.persistence;

import com.article.adapter.video.persistence.entity.VideoEntity;
import com.article.domain.media.DownloadStatus;
import com.article.domain.media.VideoPlatform;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface VideoRepository extends JpaRepository<VideoEntity, String> {

    boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId);

    boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid);

    boolean existsByArticleIdAndYoutubeVideoId(String articleId, String youtubeVideoId);

    List<VideoEntity> findByArticleIdIn(List<String> articleIds);

    List<VideoEntity> findByDownloadStatusAndPlatformInOrderById(
            DownloadStatus downloadStatus,
            List<VideoPlatform> platforms,
            Pageable pageable
    );
}
