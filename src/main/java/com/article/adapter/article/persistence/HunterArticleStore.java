package com.article.adapter.article.persistence;

import com.article.domain.Article;
import com.article.domain.Image;
import com.article.domain.NamedLink;
import com.article.domain.ParseResult;
import com.article.port.persistence.ParsedArticleStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "collector.site", havingValue = "AMERICAN_HUNTER", matchIfMissing = true)
public class HunterArticleStore implements ParsedArticleStorePort {

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

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

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

    private String serialize(List<NamedLink> links) {
        try {
            return objectMapper.writeValueAsString(links);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid Hunter article links", e);
        }
    }
}
