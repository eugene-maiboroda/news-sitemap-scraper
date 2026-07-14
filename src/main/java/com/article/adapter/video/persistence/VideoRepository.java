package com.article.adapter.video.persistence;

import com.article.adapter.video.persistence.entity.VideoEntity;
import com.article.domain.ArticleVideoRef;
import com.article.domain.CategoryPendingCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

interface VideoRepository extends JpaRepository<VideoEntity, String> {

    boolean existsByArticleIdAndJwMediaId(String articleId, String jwMediaId);

    boolean existsByArticleIdAndConnatixMmid(String articleId, String connatixMmid);

    boolean existsByArticleIdAndYoutubeVideoId(String articleId, String youtubeVideoId);

    // TODO: batch this for very large article-id lists (a single category could produce
    // tens/hundreds of thousands of ids in one IN (...) clause) — deferred per current scope.
    List<VideoEntity> findByArticleIdIn(List<String> articleIds);

    @Query("SELECT new com.article.domain.CategoryPendingCount(a.category, COUNT(v))\n" +
            "FROM VideoEntity v\n" +
            "JOIN ArticleEntity a\n" +
            "ON a.id = v.articleId\n" +
            "WHERE v.downloadStatus = 'PENDING'\n" +
            "AND v.platform IN ('JW_TOP', 'YOUTUBE')\n" +
            "AND a.category IS NOT NULL AND a.category <> ''\n" +
            "GROUP BY a.category\n" +
            "ORDER BY COUNT(v) ASC")
    List<CategoryPendingCount> findCategoriesOrderedByPendingCount();

    @Query("SELECT new com.article.domain.ArticleVideoRef(\n" +
            "    v.id,\n" +
            "    v.articleId,\n" +
            "    a.category,\n" +
            "    v.platform,\n" +
            "    v.m3u8Url,\n" +
            "    v.youtubeVideoId\n" +
            ")\n" +
            "FROM VideoEntity v\n" +
            "JOIN ArticleEntity a\n" +
            "ON a.id = v.articleId\n" +
            "WHERE v.downloadStatus = 'PENDING'\n" +
            "AND v.platform IN ('JW_TOP', 'YOUTUBE')\n" +
            "AND a.category = :category")
    List<ArticleVideoRef> findPendingVideosForCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT new com.article.domain.ArticleVideoRef(\n" +
            "    v.id,\n" +
            "    v.articleId,\n" +
            "    a.category,\n" +
            "    v.platform,\n" +
            "    v.m3u8Url,\n" +
            "    v.youtubeVideoId\n" +
            ")\n" +
            "FROM VideoEntity v\n" +
            "JOIN ArticleEntity a\n" +
            "ON a.id = v.articleId\n" +
            "WHERE v.downloadStatus = 'PENDING'\n" +
            "AND v.platform IN ('JW_TOP', 'YOUTUBE')\n" +
            "AND (a.category IS NULL OR a.category = '')")
    List<ArticleVideoRef> findPendingVideosWithoutCategory(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE VideoEntity v SET v.downloadStatus = 'DOWNLOADED', v.localFilePath = :path, v.fileSize = :size WHERE v.id = :id")
    void markDownloaded(@Param("id") String id, @Param("path") String path, @Param("size") long size);

    @Modifying
    @Transactional
    @Query("UPDATE VideoEntity v SET v.downloadStatus = 'FAILED' WHERE v.id = :id")
    void markFailed(@Param("id") String id);
}
