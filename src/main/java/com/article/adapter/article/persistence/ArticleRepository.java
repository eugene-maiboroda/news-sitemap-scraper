package com.article.adapter.article.persistence;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ArticleRepository extends JpaRepository<ArticleEntity, String> {

    List<ArticleEntity> findAllByOrderByPublishDateAscIdAsc(Pageable pageable);

    @Query(value = """
            SELECT a.*
            FROM articles a
            JOIN article_links l ON l.article_url = a.article_url
            WHERE l.collection_batch_id = :collectionBatchId
            ORDER BY a.published_date ASC, a.id ASC
            """, nativeQuery = true)
    List<ArticleEntity> findByCollectionBatchId(@Param("collectionBatchId") String collectionBatchId, Pageable pageable);
}
