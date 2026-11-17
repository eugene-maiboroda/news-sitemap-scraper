package com.article.adapter.image.persistence;

import com.article.adapter.image.persistence.entity.ImageEntity;
import com.article.domain.ArticleImageRef;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

interface ImageRepository extends JpaRepository<ImageEntity, String> {

    @Query("""
            SELECT new com.article.domain.ArticleImageRef(i.id, i.articleId, i.imageUrl, i.position)
            FROM ImageEntity i
            WHERE i.downloadStatus = com.article.domain.status.DownloadStatus.PENDING
            ORDER BY i.id
            """)
    List<ArticleImageRef> findPending(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE ImageEntity i SET i.downloadStatus = com.article.domain.status.DownloadStatus.DOWNLOADED, i.localFilePath = :path, i.fileSize = :size WHERE i.id = :id")
    void markDownloaded(@Param("id") String id, @Param("path") String path, @Param("size") long size);

    @Modifying
    @Transactional
    @Query("UPDATE ImageEntity i SET i.downloadStatus = com.article.domain.status.DownloadStatus.FAILED WHERE i.id = :id")
    void markFailed(@Param("id") String id);
}
