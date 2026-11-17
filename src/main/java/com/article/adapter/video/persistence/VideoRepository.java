package com.article.adapter.video.persistence;

import com.article.adapter.video.persistence.entity.VideoEntity;
import com.article.domain.ArticleVideoRef;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

interface VideoRepository extends JpaRepository<VideoEntity, String> {

    @Query("""
            SELECT new com.article.domain.ArticleVideoRef(i.id, i.articleId, i.sourceUrl, i.youtubeVideoId)
            FROM VideoEntity i
            WHERE i.downloadStatus = com.article.domain.status.DownloadStatus.PENDING
            ORDER BY i.id
            """)
    List<ArticleVideoRef> findPending(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE VideoEntity v SET v.downloadStatus = com.article.domain.status.DownloadStatus.DOWNLOADED, v.localFilePath = :path, v.fileSize = :size WHERE v.id = :id")
    void markDownloaded(@Param("id") String id, @Param("path") String path, @Param("size") long size);

    @Modifying
    @Transactional
    @Query("UPDATE VideoEntity v SET v.downloadStatus = com.article.domain.status.DownloadStatus.FAILED WHERE v.id = :id")
    void markFailed(@Param("id") String id);
}
