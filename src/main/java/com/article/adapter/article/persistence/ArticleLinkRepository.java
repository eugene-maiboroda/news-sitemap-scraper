package com.article.adapter.article.persistence;

import com.article.adapter.article.persistence.entity.ArticleLinkEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

interface ArticleLinkRepository extends JpaRepository<ArticleLinkEntity, UUID> {

    boolean existsByArticleUrl(String articleUrl);

    @Query("SELECT a FROM ArticleLinkEntity a WHERE a.parsed = false ORDER BY a.articleDate")
    List<ArticleLinkEntity> findUnparsedBatch(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE ArticleLinkEntity a SET a.parsed = true WHERE a.articleUrl = :url")
    void markParsed(@Param("url") String url);
}