package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;

import com.article.domain.NamedLink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class ArticleExportReader {

    private static final String ARTICLES_SQL = """
            SELECT article.id, article.article_url, article.title, article.subtitles, links.discovery_description,
                   article.published_date, article.author, article.author_link, links.menu_paths,
                   article.tags, article.affiliate_disclosure, article.content,
                   article.footer_tags, article.related_stories
            FROM articles article
            LEFT JOIN article_links links ON links.article_url = article.article_url
            ORDER BY article.published_date DESC NULLS LAST, article.article_url ASC
            LIMIT :batchSize OFFSET :offset
            """;
    private static final String IMAGES_SQL = """
            SELECT article_id, image_url, caption, position
            FROM image
            WHERE article_id IN (:ids)
            ORDER BY article_id, position
            """;
    private static final String VIDEOS_SQL = """
            SELECT article_id, source_url, position
            FROM videos
            WHERE article_id IN (:ids)
            ORDER BY article_id, position
            """;
    private static final TypeReference<Map<String, List<List<String>>>> MENU_PATHS = new TypeReference<>() {};
    private static final TypeReference<List<NamedLink>> LINKS = new TypeReference<>() {};

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public List<ArticleExportDto> readBatch(long offset, int batchSize) {
        List<ArticleRow> rows = jdbc.query(ARTICLES_SQL, Map.of("offset", offset, "batchSize", batchSize),
                (row, index) -> readArticle(row));
        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> ids = rows.stream().map(ArticleRow::id).toList();
        Map<String, List<ArticleExportDto.Photo>> images = readImages(ids);
        Map<String, List<ArticleExportDto.Video>> videos = readVideos(ids);

        return rows.stream()
                .map(row -> row.toDto(images.getOrDefault(row.id(), List.of()),
                        videos.getOrDefault(row.id(), List.of())))
                .toList();
    }

    private Map<String, List<ArticleExportDto.Photo>> readImages(List<String> ids) {
        return readMedia(IMAGES_SQL, ids, (row, index) -> new ArticleExportDto.Photo(
                optionalText(row.getString("caption")),
                row.getString("image_url"),
                row.getInt("position")
        ));
    }

    private Map<String, List<ArticleExportDto.Video>> readVideos(List<String> ids) {
        return readMedia(VIDEOS_SQL, ids, (row, index) -> new ArticleExportDto.Video(
                row.getString("source_url"),
                row.getInt("position")
        ));
    }

    private ArticleRow readArticle(ResultSet row) throws SQLException {
        return new ArticleRow(
                row.getString("id"),
                row.getString("article_url"),
                row.getString("title"),
                optionalText(row.getString("subtitles")),
                optionalText(row.getString("discovery_description")),
                row.getObject("published_date", LocalDate.class),
                optionalText(row.getString("author")),
                optionalText(row.getString("author_link")),
                readJson(row, "menu_paths", MENU_PATHS, Map.of()),
                readJson(row, "tags", LINKS, List.of()),
                optionalText(row.getString("affiliate_disclosure")),
                Objects.requireNonNullElse(row.getString("content"), ""),
                readJson(row, "footer_tags", LINKS, List.of()),
                readJson(row, "related_stories", LINKS, List.of())
        );
    }

    private <T> T readJson(ResultSet row, String column, TypeReference<T> type, T fallback) throws SQLException {
        String value = row.getString(column);
        if (value == null) {
            return fallback;
        }

        try {
            return Objects.requireNonNullElse(objectMapper.readValue(value, type), fallback);
        } catch (JsonProcessingException e) {
            throw new SQLException("Invalid " + column + " JSON for " + row.getString("article_url"), e);
        }
    }

    private <T> Map<String, List<T>> readMedia(String sql, List<String> ids, RowMapper<T> mapper) {
        return jdbc.query(sql, Map.of("ids", ids), rows -> {
            Map<String, List<T>> media = new HashMap<>();
            int index = 0;
            while (rows.next()) {
                media.computeIfAbsent(rows.getString("article_id"), id -> new ArrayList<>())
                        .add(mapper.mapRow(rows, index++));
            }
            return media;
        });
    }

    private static String optionalText(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private record ArticleRow(

            String id,

            String articleUrl,

            String title,

            String subtitles,

            String description,

            LocalDate publishedDate,

            String author,

            String authorLink,

            Map<String, List<List<String>>> menuPaths,

            List<NamedLink> tags,

            String affiliateDisclosure,

            String content,

            List<NamedLink> footerTags,

            List<NamedLink> relatedStories
    ) {

        private ArticleExportDto toDto(
                List<ArticleExportDto.Photo> images, List<ArticleExportDto.Video> videos) {
            return new ArticleExportDto(articleUrl, title, subtitles, description, publishedDate, author, authorLink,
                    menuPaths, tags, images, videos, affiliateDisclosure, content, footerTags, relatedStories);
        }
    }
}
