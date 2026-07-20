package com.article.adapter.article.persistence;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ArticleRepository extends JpaRepository<ArticleEntity, String> {

    List<ArticleEntity> findAllByOrderByPublishDateAscIdAsc(Pageable pageable);
}
