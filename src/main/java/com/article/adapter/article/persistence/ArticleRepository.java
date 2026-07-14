package com.article.adapter.article.persistence;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ArticleRepository extends JpaRepository<ArticleEntity, String> {

    @Query("SELECT DISTINCT a.category FROM ArticleEntity a WHERE a.category IS NOT NULL AND a.category <> ''")
    List<String> findDistinctCategories();

    // ordered by id so that LIMIT/OFFSET paging is stable across calls — without an explicit
    // order Postgres doesn't guarantee row order between two queries with different offsets.
    List<ArticleEntity> findByCategoryOrderById(String category, Pageable pageable);

    @Query("SELECT a FROM ArticleEntity a WHERE (a.category IS NULL OR a.category = '') ORDER BY a.id")
    List<ArticleEntity> findArticlesWithoutCategory(Pageable pageable);

    // native + interface projection, deliberately NOT mapped onto ArticleEntity — raw_html must
    // never become a selected column of the entity used everywhere else (parsing, pagination),
    // that's exactly the memory blowup that was removed from Article/ArticleEntity previously.
    @Query(value = "SELECT id, raw_html AS rawHtml FROM articles WHERE id IN (:ids)", nativeQuery = true)
    List<ArticleRawHtmlView> findRawHtmlByIdIn(@Param("ids") List<String> ids);

    @Query(value = "SELECT id, raw_html AS rawHtml FROM articles " +
            "WHERE topics_extracted = FALSE AND raw_html IS NOT NULL " +
            "ORDER BY id LIMIT :limit", nativeQuery = true)
    List<ArticleRawHtmlView> findPendingTopicsBatch(@Param("limit") int limit);

    @Query(value = """
            -- WHY LATERAL (not UNION ALL of 5 SELECTs): each UNION ALL branch independently
            -- re-scans the full articles table once per topic level (5 scans total); LATERAL
            -- VALUES fans each row out into up to 5 (level, url, name) candidates within ONE
            -- scan. Measured on this table (~1.3M rows): UNION ALL 13.6s vs LATERAL 2.9s.
            --
            -- WHY DISTINCT on the full truncated tuple (not just topic_url): the same topic_url
            -- genuinely has DIFFERENT hierarchies across articles (verified on real data — e.g.
            -- "new-york-liberty" appears under Sports/WNBA, Post Sports+, AND Sports Betting;
            -- "tag/exclusive" spans 8+ unrelated main categories). Each distinct hierarchy must
            -- become its own export file, so the grouping key is the full tuple, not the URL alone.
            --
            -- WHY the CASE truncation before DISTINCT: without it, a level=1 target would be
            -- fragmented into spurious extra "distinct" rows purely from irrelevant topic2..5_name
            -- variation unrelated to the level this particular row is contributing at.
            SELECT DISTINCT
                   topic_url AS topicUrl, level AS level, main_category AS mainCategory,
                   CASE WHEN level >= 1 THEN topic1_name END AS topic1Name,
                   CASE WHEN level >= 2 THEN topic2_name END AS topic2Name,
                   CASE WHEN level >= 3 THEN topic3_name END AS topic3Name,
                   CASE WHEN level >= 4 THEN topic4_name END AS topic4Name,
                   CASE WHEN level >= 5 THEN topic5_name END AS topic5Name
            FROM articles a
            CROSS JOIN LATERAL (VALUES
                (1, a.topic1_url, a.topic1_name),
                (2, a.topic2_url, a.topic2_name),
                (3, a.topic3_url, a.topic3_name),
                (4, a.topic4_url, a.topic4_name),
                (5, a.topic5_url, a.topic5_name)
            ) AS lvl(level, topic_url, topic_name)
            WHERE lvl.topic_url IS NOT NULL AND lvl.topic_name IS NOT NULL
            ORDER BY topicUrl, level, mainCategory
            """, nativeQuery = true)
    List<TopicTargetRow> findDistinctTopicTargetRows();

    // Isolated edge case, additive to the discovery query above (never touches it): the discovery
    // query above requires topic1_name to be non-null, so it misses standalone sections that
    // render as a logo/icon with no topic text at all (e.g. "Post Sports+" — topic1_url is
    // present, topic1_name is null, and there is no deeper topic1..5 hierarchy either).
    //
    // buildHierarchy(mainCategory, [null]) already correctly yields [mainCategory] with no change
    // needed there. But membership does need a DEDICATED query (findStandaloneByTopic1 below), not
    // the generic findByTopic1: findByTopic1 deliberately ignores topic2..5 (an article tagged
    // NFL+Cowboys must appear in both the NFL and Cowboys files — intentional overlap), so reusing
    // it here would also pull in every article that has this same topic1 but a REAL deeper topic
    // (e.g. "Post Sports+ -> Inside the Knicks") — those already have their own correct target via
    // the main discovery query above and must not be duplicated into this standalone bucket.
    // Verified against real data: findByTopic1 alone matches 9,141 rows here, but only 2,764 of
    // them have truly no hierarchy beyond the topic1 badge — the rest belong to deeper targets.
    @Query(value = """
            SELECT DISTINCT topic1_url AS topicUrl, main_category AS mainCategory
            FROM articles
            WHERE topic1_url IS NOT NULL AND topic1_name IS NULL
              AND topic2_url IS NULL AND topic2_name IS NULL
              AND topic3_url IS NULL AND topic3_name IS NULL
              AND topic4_url IS NULL AND topic4_name IS NULL
              AND topic5_url IS NULL AND topic5_name IS NULL
              AND main_category IS NOT NULL
            """, nativeQuery = true)
    List<StandaloneTopicRow> findStandaloneTopicRows();

    // Exact-match counterpart to findStandaloneTopicRows() above — mirrors its WHERE clause
    // precisely (topic1 has a URL but no name, and topic2..5 are entirely empty), so membership
    // for this bucket is exactly the rows discovery found, no more.
    @Query("SELECT a FROM ArticleEntity a WHERE a.topic1Url = :url " +
            "AND (a.mainCategory = :mc OR (:mc IS NULL AND a.mainCategory IS NULL)) " +
            "AND a.topic1Name IS NULL " +
            "AND a.topic2Url IS NULL AND a.topic2Name IS NULL " +
            "AND a.topic3Url IS NULL AND a.topic3Name IS NULL " +
            "AND a.topic4Url IS NULL AND a.topic4Name IS NULL " +
            "AND a.topic5Url IS NULL AND a.topic5Name IS NULL " +
            "ORDER BY a.id")
    List<ArticleEntity> findStandaloneByTopic1(@Param("url") String url, @Param("mc") String mc, Pageable pageable);

    // Each of the 5 membership queries below checks the FULL chain of topic names up to and
    // including its own level (not just the preceding levels) — a target is identified by
    // topicUrl + level + mainCategory + topic1..topicLevel names together, the same tuple the
    // discovery query above groups by. Null-safe comparisons throughout: mainCategory and
    // intermediate topic names can legitimately be null (e.g. a Post Sports+ badge renders as a
    // logo with no topic1 text, so topic1Name is null while topic2Name is real).
    //
    // WHY 5 small methods instead of one CASE/OR "monster query": each reads independently and is
    // trivially checkable; a single branching query accumulating null-safe ANDs across 5 levels
    // is harder to verify by eye. WHY not a broad "any level matches this url" fetch + Java-side
    // filter: that breaks clean pagination — "is there more data" would stop reflecting the real
    // per-target result once filtering happens after the page is already fetched.

    @Query("SELECT a FROM ArticleEntity a WHERE a.topic1Url = :url " +
            "AND (a.mainCategory = :mc OR (:mc IS NULL AND a.mainCategory IS NULL)) " +
            "AND (a.topic1Name = :t1 OR (:t1 IS NULL AND a.topic1Name IS NULL)) ORDER BY a.id")
    List<ArticleEntity> findByTopic1(@Param("url") String url, @Param("mc") String mc,
                                      @Param("t1") String t1, Pageable pageable);

    @Query("SELECT a FROM ArticleEntity a WHERE a.topic2Url = :url " +
            "AND (a.mainCategory = :mc OR (:mc IS NULL AND a.mainCategory IS NULL)) " +
            "AND (a.topic1Name = :t1 OR (:t1 IS NULL AND a.topic1Name IS NULL)) " +
            "AND (a.topic2Name = :t2 OR (:t2 IS NULL AND a.topic2Name IS NULL)) ORDER BY a.id")
    List<ArticleEntity> findByTopic2(@Param("url") String url, @Param("mc") String mc,
                                      @Param("t1") String t1, @Param("t2") String t2, Pageable pageable);

    @Query("SELECT a FROM ArticleEntity a WHERE a.topic3Url = :url " +
            "AND (a.mainCategory = :mc OR (:mc IS NULL AND a.mainCategory IS NULL)) " +
            "AND (a.topic1Name = :t1 OR (:t1 IS NULL AND a.topic1Name IS NULL)) " +
            "AND (a.topic2Name = :t2 OR (:t2 IS NULL AND a.topic2Name IS NULL)) " +
            "AND (a.topic3Name = :t3 OR (:t3 IS NULL AND a.topic3Name IS NULL)) ORDER BY a.id")
    List<ArticleEntity> findByTopic3(@Param("url") String url, @Param("mc") String mc,
                                      @Param("t1") String t1, @Param("t2") String t2,
                                      @Param("t3") String t3, Pageable pageable);

    @Query("SELECT a FROM ArticleEntity a WHERE a.topic4Url = :url " +
            "AND (a.mainCategory = :mc OR (:mc IS NULL AND a.mainCategory IS NULL)) " +
            "AND (a.topic1Name = :t1 OR (:t1 IS NULL AND a.topic1Name IS NULL)) " +
            "AND (a.topic2Name = :t2 OR (:t2 IS NULL AND a.topic2Name IS NULL)) " +
            "AND (a.topic3Name = :t3 OR (:t3 IS NULL AND a.topic3Name IS NULL)) " +
            "AND (a.topic4Name = :t4 OR (:t4 IS NULL AND a.topic4Name IS NULL)) ORDER BY a.id")
    List<ArticleEntity> findByTopic4(@Param("url") String url, @Param("mc") String mc,
                                      @Param("t1") String t1, @Param("t2") String t2,
                                      @Param("t3") String t3, @Param("t4") String t4, Pageable pageable);

    @Query("SELECT a FROM ArticleEntity a WHERE a.topic5Url = :url " +
            "AND (a.mainCategory = :mc OR (:mc IS NULL AND a.mainCategory IS NULL)) " +
            "AND (a.topic1Name = :t1 OR (:t1 IS NULL AND a.topic1Name IS NULL)) " +
            "AND (a.topic2Name = :t2 OR (:t2 IS NULL AND a.topic2Name IS NULL)) " +
            "AND (a.topic3Name = :t3 OR (:t3 IS NULL AND a.topic3Name IS NULL)) " +
            "AND (a.topic4Name = :t4 OR (:t4 IS NULL AND a.topic4Name IS NULL)) " +
            "AND (a.topic5Name = :t5 OR (:t5 IS NULL AND a.topic5Name IS NULL)) ORDER BY a.id")
    List<ArticleEntity> findByTopic5(@Param("url") String url, @Param("mc") String mc,
                                      @Param("t1") String t1, @Param("t2") String t2, @Param("t3") String t3,
                                      @Param("t4") String t4, @Param("t5") String t5, Pageable pageable);

    interface ArticleRawHtmlView {
        String getId();

        String getRawHtml();
    }

    interface TopicTargetRow {
        String getTopicUrl();

        Integer getLevel();

        String getMainCategory();

        String getTopic1Name();

        String getTopic2Name();

        String getTopic3Name();

        String getTopic4Name();

        String getTopic5Name();
    }

    interface StandaloneTopicRow {
        String getTopicUrl();

        String getMainCategory();
    }
}
