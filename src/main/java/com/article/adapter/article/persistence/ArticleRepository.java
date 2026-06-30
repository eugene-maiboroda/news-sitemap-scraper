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

    @Query("SELECT new com.article.domain.ArticleImageRef(a.id, a.category, a.imageUrl) " +
           "FROM ArticleEntity a WHERE a.imageUrl IS NOT NULL ORDER BY a.publishDate DESC")
    List<ArticleImageRef> findImageRefsForDelivery(Pageable pageable);
}
