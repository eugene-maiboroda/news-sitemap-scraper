package com.article.adapter.article.persistence;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.domain.Author;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ArticleRepository extends JpaRepository<ArticleEntity, String> {

    @Query(value = """
            SELECT DISTINCT ON (author_link)
               author      AS authorName,
               author_link AS authorUrl
           FROM articles
           WHERE author_link IS NOT NULL
             AND BTRIM(author_link) <> ''
             AND author IS NOT NULL
             AND BTRIM(author) <> ''
           ORDER BY author_link, id;
            """, nativeQuery = true)
    List<DistinctAuthorView> findDistinctAuthors();

    @Query("SELECT DISTINCT a.category FROM ArticleEntity a WHERE a.category IS NOT NULL AND a.category <> ''")
    List<String> findDistinctCategories();

    // ordered by id so that LIMIT/OFFSET paging is stable across calls — without an explicit
    // order Postgres doesn't guarantee row order between two queries with different offsets.
    List<ArticleEntity> findByCategoryOrderById(String category, Pageable pageable);

    // Full-table scan regardless of category. JSONB topics/tags are mapped with the entity, so the
    // export layer receives article data and metadata in one database round-trip per page.
    List<ArticleEntity> findAllByOrderById(Pageable pageable);

    @Query("SELECT a FROM ArticleEntity a WHERE (a.category IS NULL OR a.category = '') ORDER BY a.id")
    List<ArticleEntity> findArticlesWithoutCategory(Pageable pageable);

    @Query(value = "SELECT id, raw_html AS rawHtml FROM articles " +
            "WHERE topics_extracted = FALSE AND raw_html IS NOT NULL " +
            "ORDER BY id LIMIT :limit", nativeQuery = true)
    List<ArticleRawHtmlView> findPendingTopicsBatch(@Param("limit") int limit);

    @Query(value = """
            SELECT a.*
            FROM articles a
            WHERE a.topics_extracted = TRUE
              AND a.main_category IS NOT DISTINCT FROM :mainCategory
              AND a.topics @> jsonb_build_array(jsonb_build_object('url', :topicUrl))
              AND (a.topics -> CAST(:topicIndex AS integer)) ->> 'url' = :topicUrl
              AND (
                    SELECT jsonb_agg(item.value -> 'name' ORDER BY item.ordinality)
                    FROM jsonb_array_elements(a.topics) WITH ORDINALITY item(value, ordinality)
                    WHERE item.ordinality <= :level
                  ) = CAST(:topicNames AS jsonb)
              AND (:standalone = FALSE OR jsonb_array_length(a.topics) = 1)
            ORDER BY a.id
            """, nativeQuery = true)
    List<ArticleEntity> findByTopicTarget(
            @Param("topicUrl") String topicUrl,
            @Param("topicIndex") int topicIndex,
            @Param("level") int level,
            @Param("mainCategory") String mainCategory,
            @Param("topicNames") String topicNames,
            @Param("standalone") boolean standalone,
            Pageable pageable);

    interface ArticleRawHtmlView {
        String getId();

        String getRawHtml();
    }

    interface DistinctAuthorView {

        String getAuthorName();

        String getAuthorUrl();
    }

}
