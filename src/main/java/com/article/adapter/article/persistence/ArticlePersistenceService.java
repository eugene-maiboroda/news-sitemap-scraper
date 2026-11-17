package com.article.adapter.article.persistence;

import com.article.adapter.article.mapper.ArticleMapper;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.domain.Author;
import com.article.domain.DiscoveredArticle;
import com.article.domain.Image;
import com.article.domain.NamedLink;
import com.article.domain.ParseResult;
import com.article.domain.status.CrawlStatus;
import com.article.port.IdGenerationPort;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.persistence.ParsedArticlePersistencePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticlePersistenceService implements ArticlePersistencePort, ParsedArticlePersistencePort {

    private static final int DISCOVERY_BATCH_SIZE = 500;

    private static final String BULK_INSERT_SQL = """
            INSERT INTO article_links (id, article_url, article_date, status)
            VALUES (?, ?, ?, 'NEW')
            ON CONFLICT (article_url) DO NOTHING
            """;

    private static final String UPSERT_DISCOVERED_ARTICLE_SQL = """
            INSERT INTO article_links (
                id, article_url, status, discovery_title, discovery_description, menu_paths
            )
            VALUES (?, ?, 'NEW', ?, ?, CAST(? AS jsonb))
            ON CONFLICT (article_url) DO UPDATE SET
                discovery_title = COALESCE(NULLIF(article_links.discovery_title, ''), EXCLUDED.discovery_title),
                discovery_description = COALESCE(
                    NULLIF(article_links.discovery_description, ''), EXCLUDED.discovery_description
                ),
                menu_paths = (
                    SELECT COALESCE(jsonb_object_agg(paths.section_url, paths.hierarchies), '{}'::jsonb)
                    FROM (
                        SELECT section_url, jsonb_agg(DISTINCT hierarchy ORDER BY hierarchy) AS hierarchies
                        FROM (
                            SELECT key AS section_url, value AS section_paths
                            FROM jsonb_each(COALESCE(article_links.menu_paths, '{}'::jsonb))
                            UNION ALL
                            SELECT key, value FROM jsonb_each(EXCLUDED.menu_paths)
                        ) AS sections
                        CROSS JOIN LATERAL jsonb_array_elements(section_paths) AS entries(hierarchy)
                        GROUP BY section_url
                    ) AS paths
                )
            """;

    private static final String YOUTUBE_WATCH_PREFIX = "https://www.youtube.com/watch?v=";

    private static final String UPSERT_ARTICLE_SQL = """
            INSERT INTO articles (
                id, article_url, title, published_date, author, author_link, content, raw_html,
                affiliate_disclosure, tags, footer_tags, related_stories, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                article_url = EXCLUDED.article_url,
                title = EXCLUDED.title,
                published_date = EXCLUDED.published_date,
                author = EXCLUDED.author,
                author_link = EXCLUDED.author_link,
                content = EXCLUDED.content,
                raw_html = EXCLUDED.raw_html,
                affiliate_disclosure = EXCLUDED.affiliate_disclosure,
                tags = EXCLUDED.tags,
                footer_tags = EXCLUDED.footer_tags,
                related_stories = EXCLUDED.related_stories
            """;

    private final ArticleLinkRepository linkRepository;
    private final ArticleMapper articleMapper;
    private final JdbcTemplate jdbcTemplate;
    private final IdGenerationPort idGenerationPort;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void insertLinks(List<ArticleLink> links) {
        long start = System.currentTimeMillis();
        jdbcTemplate.batchUpdate(BULK_INSERT_SQL, links, 1000, (ps, link) -> {
            ps.setString(1, idGenerationPort.generateId());
            ps.setString(2, link.articleUrl());
            ps.setObject(3, link.articleDate());
        });
        log.info("Insert took {} ms", System.currentTimeMillis() - start);
    }

    @Override
    @Transactional
    public void saveDiscoveredArticles(List<DiscoveredArticle> articles) {
        List<DiscoveredArticle> articlesByUrl = articles.stream()
                .sorted(Comparator.comparing(DiscoveredArticle::articleUrl))
                .toList();

        jdbcTemplate.batchUpdate(UPSERT_DISCOVERED_ARTICLE_SQL, articlesByUrl, DISCOVERY_BATCH_SIZE, (statement, article) -> {
            statement.setString(1, idGenerationPort.generateId());
            statement.setString(2, article.articleUrl());
            statement.setString(3, article.title());
            statement.setString(4, article.description());
            statement.setString(5, serializeMenuPaths(article));
        });
    }

    @Override
    public List<ArticleLink> findNewBatch(int limit) {
        return articleMapper.toArticleLinks(
                linkRepository.findByStatusOrderByArticleDate(CrawlStatus.NEW, PageRequest.of(0, limit)));
    }

    @Override
    @Transactional
    public void markError(String articleUrl, String errorMessage) {
        linkRepository.updateStatusWithError(articleUrl, CrawlStatus.ERROR, errorMessage);
    }

    @Override
    public List<Author> findDistinctAuthors() {
        return jdbcTemplate.query("""
                SELECT DISTINCT ON (btrim(author_link))
                    btrim(author_link) AS profile_url, author
                FROM articles
                WHERE btrim(author_link) ~* '^https?://[^/]+'
                ORDER BY btrim(author_link), NULLIF(btrim(author), '') NULLS LAST, id
                """, (row, index) -> new Author(
                null,
                row.getString("author"),
                row.getString("profile_url"),
                null,
                null,
                null
        ));
    }

    @Override
    @Transactional
    public void save(ParseResult.MenuArticleSuccess result) {
        Article article = result.article();
        // Serialize reprocessing of the same URL before replacing its media rows.
        jdbcTemplate.queryForObject("SELECT id FROM article_links WHERE article_url = ? FOR UPDATE",
                String.class, article.url());

        upsertArticle(result);
        replaceMedia(article.id(), result.images(), result.videoUrls());

        jdbcTemplate.update(
                "UPDATE article_links SET status = 'PARSED', error_message = NULL WHERE article_url = ?",
                article.url()
        );
    }

    @Override
    public List<ArchivedArticle> findCaptionBackfillBatch(String afterId, int limit) {
        return jdbcTemplate.query("""
                SELECT a.id, a.article_url, a.raw_html
                FROM articles a
                WHERE a.id > ? AND a.raw_html IS NOT NULL AND a.article_url IS NOT NULL
                  AND EXISTS (
                      SELECT 1 FROM image i
                      WHERE i.article_id = a.id AND (i.caption IS NULL OR i.caption ~ '^[[:space:]]*$')
                  )
                ORDER BY a.id
                LIMIT ?
                """, (row, index) -> new ArchivedArticle(row.getString("id"), row.getString("article_url"),
                row.getString("raw_html")), afterId, limit);
    }

    @Override
    public int fillMissingCaption(Image image) {
        return jdbcTemplate.update("""
                UPDATE image i SET caption = ?
                WHERE i.article_id = ? AND i.image_url = ? AND i.position = ?
                  AND (i.caption IS NULL OR i.caption ~ '^[[:space:]]*$')
                  AND NOT EXISTS (
                      SELECT 1 FROM image other
                      WHERE other.article_id = i.article_id AND other.position = i.position AND other.id <> i.id
                  )
                """, image.getCaption(), image.getArticleId(), image.getImageUrl(), image.getPosition());
    }

    private void upsertArticle(ParseResult.MenuArticleSuccess result) {
        Article article = result.article();
        String tagsJson = serialize(result.tags());
        String footerTagsJson = serialize(result.footerTags());
        String relatedStoriesJson = serialize(result.relatedStories());

        jdbcTemplate.update(
                UPSERT_ARTICLE_SQL,
                article.id(),
                article.url(),
                article.title(),
                article.publishDate(),
                article.author(),
                article.authorLink(),
                article.content(),
                result.rawHtml(),
                result.affiliateDisclosure(),
                tagsJson,
                footerTagsJson,
                relatedStoriesJson
        );
    }

    private void replaceMedia(String articleId, List<Image> images, List<String> videoUrls) {
        jdbcTemplate.update("DELETE FROM image WHERE article_id = ?", articleId);
        jdbcTemplate.update("DELETE FROM videos WHERE article_id = ?", articleId);

        for (Image image : images) {
            int position = image.getPosition();
            String imageId = articleId + "_img_" + position;
            jdbcTemplate.update(
                    """
                    INSERT INTO image (id, article_id, image_url, caption, position, download_status, created_at)
                    VALUES (?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)
                    """, imageId, articleId, image.getImageUrl(), image.getCaption(), position);
        }

        for (int position = 0; position < videoUrls.size(); position++) {
            String url = videoUrls.get(position);
            String videoId = articleId + "_video_" + position;
            String youtubeId = url.startsWith(YOUTUBE_WATCH_PREFIX)
                    ? url.substring(YOUTUBE_WATCH_PREFIX.length()) : null;
            String platform = youtubeId == null ? null : "YOUTUBE";

            jdbcTemplate.update(
                    """
                    INSERT INTO videos (id, article_id, source_url, position, platform,
                                        youtube_video_id, download_status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)
                    """, videoId, articleId, url, position, platform, youtubeId);
        }
    }

    private String serializeMenuPaths(DiscoveredArticle article) {
        try {
            return objectMapper.writeValueAsString(article.menuPaths());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid menu paths for " + article.articleUrl(), e);
        }
    }

    private String serialize(List<NamedLink> links) {
        try {
            return objectMapper.writeValueAsString(links);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid article links", e);
        }
    }
}
