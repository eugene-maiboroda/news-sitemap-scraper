package com.article.adapter.image.persistence;

import com.article.adapter.image.persistence.entity.ImageEntity;
import com.article.domain.media.DownloadStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ImageRepository extends JpaRepository<ImageEntity, String> {

    List<ImageEntity> findByArticleIdIn(List<String> articleIds);

    List<ImageEntity> findByDownloadStatusOrderById(DownloadStatus status, Pageable pageable);
}
