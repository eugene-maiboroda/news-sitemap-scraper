package com.article.adapter.article.persistence;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.domain.ArticleImageRef;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface ArticleRepository extends JpaRepository<ArticleEntity, String> {

    @Query("SELECT a FROM ArticleEntity a ORDER BY a.publishDate DESC")
    List<ArticleEntity> findAllOrdered();

    // ArticleEntity no longer has imageUrl (moved to the separate `image` table/Image entity) —
    // kept as a structural no-op so ImageDownloadPort/ImageDownloadAdapter still compile and boot;
    // they'll be rewired to the new Image entity in a later phase.
    @Query("SELECT new com.article.domain.ArticleImageRef(\n" +
            "    a.id,\n" +
            "    a.category,\n" +
            "    i.imageUrl,\n" +
            "    i.position\n" +
            ")\n" +
            "FROM ImageEntity i\n" +
            "JOIN ArticleEntity a\n" +
            "ON a.id = i.articleId\n" +
            "WHERE i.downloadStatus = 'PENDING'")
    List<ArticleImageRef> findImageRefsForDelivery(Pageable pageable);
}
