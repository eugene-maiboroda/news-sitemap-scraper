package com.article.adapter.article.persistence;

import com.article.adapter.article.persistence.entity.LinkArticleEntity;
import com.article.adapter.article.persistence.entity.CrawlStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ArticleLinkRepository extends JpaRepository<LinkArticleEntity, String> {

    List<LinkArticleEntity> findByStatusOrderByArticleDate(CrawlStatus status, Pageable pageable);

    Optional<LinkArticleEntity> findByArticleUrl(String articleUrl);
}
