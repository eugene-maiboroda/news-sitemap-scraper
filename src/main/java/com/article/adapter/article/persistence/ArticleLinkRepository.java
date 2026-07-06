package com.article.adapter.article.persistence;

import com.article.adapter.article.persistence.entity.LinkArticleEntity;
import com.article.domain.status.CrawlStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

interface ArticleLinkRepository extends JpaRepository<LinkArticleEntity, String> {

    List<LinkArticleEntity> findByStatusOrderByArticleDate(CrawlStatus status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE LinkArticleEntity a SET a.status = :status WHERE a.articleUrl = :url")
    void updateStatus(@Param("url") String url, @Param("status") CrawlStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE LinkArticleEntity a SET a.status = :status, a.errorMessage = :msg WHERE a.articleUrl = :url")
    void updateStatusWithError(@Param("url") String url, @Param("status") CrawlStatus status, @Param("msg") String msg);
}
