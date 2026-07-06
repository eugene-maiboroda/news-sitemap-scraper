package com.article.adapter.video.persistence;

import com.article.adapter.video.persistence.entity.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface VideoRepository extends JpaRepository<VideoEntity, String> {

    boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId);

    boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid);

    boolean existsByArticleIdAndYoutubeVideoId(String articleId, String youtubeVideoId);
}
