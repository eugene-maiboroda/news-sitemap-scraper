package com.article.adapter.image.persistence;

import com.article.adapter.image.persistence.entity.ImageEntity;
import com.article.domain.ArticleImageRef;
import com.article.domain.CategoryPendingCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

interface ImageRepository extends JpaRepository<ImageEntity, String> {

    boolean existsByArticleIdAndImageUrl(String articleId, String imageUrl);

    // TODO: batch this for very large article-id lists (a single category could produce
    // tens/hundreds of thousands of ids in one IN (...) clause) — deferred per current scope.
    List<ImageEntity> findByArticleIdIn(List<String> articleIds);

    @Query("SELECT new com.article.domain.CategoryPendingCount(a.category, COUNT(i))\n" +
            "FROM ImageEntity i\n" +
            "JOIN ArticleEntity a\n" +
            "ON a.id = i.articleId\n" +
            "WHERE i.downloadStatus = 'PENDING'\n" +
            "AND a.category IS NOT NULL AND a.category <> ''\n" +
            "GROUP BY a.category\n" +
            "ORDER BY COUNT(i) ASC")
    List<CategoryPendingCount> findCategoriesOrderedByPendingCount();

    @Query("SELECT new com.article.domain.ArticleImageRef(\n" +
            "    i.id,\n" +
            "    i.articleId,\n" +
            "    a.category,\n" +
            "    i.imageUrl,\n" +
            "    i.position\n" +
            ")\n" +
            "FROM ImageEntity i\n" +
            "JOIN ArticleEntity a\n" +
            "ON a.id = i.articleId\n" +
            "WHERE i.downloadStatus = 'PENDING'\n" +
            "AND a.category = :category")
    List<ArticleImageRef> findPendingImagesForCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT new com.article.domain.ArticleImageRef(\n" +
            "    i.id,\n" +
            "    i.articleId,\n" +
            "    a.category,\n" +
            "    i.imageUrl,\n" +
            "    i.position\n" +
            ")\n" +
            "FROM ImageEntity i\n" +
            "JOIN ArticleEntity a\n" +
            "ON a.id = i.articleId\n" +
            "WHERE i.downloadStatus = 'PENDING'\n" +
            "AND (a.category IS NULL OR a.category = '')")
    List<ArticleImageRef> findPendingImagesWithoutCategory(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE ImageEntity i SET i.downloadStatus = 'DOWNLOADED', i.localFilePath = :path, i.fileSize = :size WHERE i.id = :id")
    void markDownloaded(@Param("id") String id, @Param("path") String path, @Param("size") long size);

    @Modifying
    @Transactional
    @Query("UPDATE ImageEntity i SET i.downloadStatus = 'FAILED' WHERE i.id = :id")
    void markFailed(@Param("id") String id);
}
