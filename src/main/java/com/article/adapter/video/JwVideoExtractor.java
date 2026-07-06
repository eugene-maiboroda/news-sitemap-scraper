package com.article.adapter.video;

import com.article.domain.status.DownloadStatus;
import com.article.domain.Video;
import com.article.domain.status.VideoPlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts JW Player "top video" metadata straight from the article's already-fetched
 * {@link Document} — no extra HTTP request, no browser. Two independent embedding mechanisms
 * are known to occur on NYPost pages, tried in order:
 * <ol>
 *   <li>schema.org {@code VideoObject} JSON-LD ({@link #extractFromJsonLd});</li>
 *   <li>an inline {@code jwplayer(...).setup({playlist: [...]})} call, used on older pages that
 *       don't declare the video in JSON-LD at all ({@link #extractFromInlineJwPlayer}).</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwVideoExtractor implements VideoExtractor {

    private static final Pattern MANIFEST_ID = Pattern.compile("cdn\\.jwplayer\\.com/manifests/([A-Za-z0-9]{8})\\.m3u8");
    private static final Pattern EMBED_ID = Pattern.compile("jwplatform\\.com/players/([A-Za-z0-9]{8})-");
    private static final Pattern CONTENT_ID = Pattern.compile("cdn\\.jwplayer\\.com/videos/([A-Za-z0-9]{8})-");
    private static final Pattern SETUP_CALL = Pattern.compile("jwplayer\\([^)]*\\)\\.setup\\(");

    private final ObjectMapper objectMapper;

    @Override
    public Optional<Video> extract(Document document, String articleId, String articleUrl) {
        Optional<Video> fromJsonLd = extractFromJsonLd(document, articleId);
        if (fromJsonLd.isPresent()) {
            return fromJsonLd;
        }
        return extractFromInlineJwPlayer(document, articleId, articleUrl);
    }

    // ────────────────────────────────────────────────────────────────
    // Branch 1: schema.org VideoObject JSON-LD
    // ────────────────────────────────────────────────────────────────

    private Optional<Video> extractFromJsonLd(Document doc, String articleId) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            JsonNode videoObject = findVideoObject(script.data());
            if (videoObject != null) {
                Optional<Video> video = toVideoFromJsonLd(videoObject, articleId);
                if (video.isPresent()) {
                    return video;
                }
            }
        }
        return Optional.empty();
    }

    private JsonNode findVideoObject(String jsonLd) {
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonLd);
        } catch (Exception e) {
            return null;
        }

        if (root.isArray()) {
            for (JsonNode item : root) {
                JsonNode found = asVideoObject(item);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        return asVideoObject(root);
    }

    private JsonNode asVideoObject(JsonNode item) {
        if (item == null || item.isMissingNode()) {
            return null;
        }
        if ("VideoObject".equals(item.path("@type").asText())) {
            return item;
        }
        JsonNode video = item.path("video");
        if (video.isObject() && "VideoObject".equals(video.path("@type").asText())) {
            return video;
        }
        return null;
    }

    private Optional<Video> toVideoFromJsonLd(JsonNode videoNode, String articleId) {
        String embedUrl = videoNode.path("embedUrl").asText("");
        String contentUrl = videoNode.path("contentUrl").asText("");

        String mediaId = extractMediaId(contentUrl, MANIFEST_ID)
                .or(() -> extractMediaId(embedUrl, EMBED_ID))
                .or(() -> extractMediaId(contentUrl, CONTENT_ID))
                .orElse(null);

        if (mediaId == null) {
            return Optional.empty();
        }

        return Optional.of(buildVideo(
                articleId,
                mediaId,
                textOrNull(videoNode, "name"),
                textOrNull(videoNode, "description"),
                thumbnailUrl(videoNode),
                parseDurationSeconds(textOrNull(videoNode, "duration")),
                parseInstant(textOrNull(videoNode, "uploadDate"))
        ));
    }

    // ────────────────────────────────────────────────────────────────
    // Branch 2: inline jwplayer(...).setup({ playlist: [...] })
    // ────────────────────────────────────────────────────────────────

    private Optional<Video> extractFromInlineJwPlayer(Document doc, String articleId, String articleUrl) {
        for (Element script : doc.select("script:not([type=application/ld+json])")) {
            String body = script.data();
            if (body.isBlank() || !body.contains("jwplayer(")) {
                continue;
            }
            JsonNode setup = extractSetupPayload(body);
            if (setup == null) {
                continue;
            }
            Optional<Video> video = toVideoFromPlaylist(setup.path("playlist"), articleId, articleUrl);
            if (video.isPresent()) {
                return video;
            }
        }
        return Optional.empty();
    }

    private JsonNode extractSetupPayload(String scriptBody) {
        Matcher m = SETUP_CALL.matcher(scriptBody);
        if (!m.find()) {
            return null;
        }
        String json = extractBalancedJson(scriptBody, m.end());
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    /** Scans forward from {@code fromIndex} for the first {@code {...}} block, tracking brace depth. */
    private String extractBalancedJson(String text, int fromIndex) {
        int start = text.indexOf('{', fromIndex);
        if (start == -1) {
            return null;
        }
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    /**
     * Picks the playlist item that is this article's own video, not a recommendation-feed item:
     * it must not carry a {@code feedid} (JW's recommendation feeds set one; the article's own
     * video doesn't), and its {@code link} must point back to this article.
     */
    private Optional<Video> toVideoFromPlaylist(JsonNode playlist, String articleId, String articleUrl) {
        if (!playlist.isArray()) {
            return Optional.empty();
        }
        for (JsonNode item : playlist) {
            if (item.has("feedid")) {
                continue;
            }
            if (!linkMatchesArticle(textOrNull(item, "link"), articleUrl)) {
                continue;
            }
            String mediaId = textOrNull(item, "mediaid");
            if (mediaId == null) {
                continue;
            }
            JsonNode duration = item.path("duration");
            return Optional.of(buildVideo(
                    articleId,
                    mediaId,
                    textOrNull(item, "title"),
                    textOrNull(item, "description"),
                    textOrNull(item, "image"),
                    duration.isMissingNode() ? null : duration.asInt(),
                    null
            ));
        }
        return Optional.empty();
    }

    private boolean linkMatchesArticle(String link, String articleUrl) {
        if (link == null || link.isBlank()) {
            return false;
        }
        return normalizeUrl(link).equals(normalizeUrl(articleUrl));
    }

    private String normalizeUrl(String url) {
        return url.trim().replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    // ────────────────────────────────────────────────────────────────
    // Shared
    // ────────────────────────────────────────────────────────────────

    private Video buildVideo(String articleId, String mediaId, String title, String description,
                              String thumbnailUrl, Integer durationSeconds, Instant publishedAt) {
        return Video.builder()
                .articleId(articleId)
                .platform(VideoPlatform.JW_TOP)
                .jwMediaId(mediaId)
                .m3u8Url("https://cdn.jwplayer.com/manifests/" + mediaId + ".m3u8")
                .downloadStatus(DownloadStatus.PENDING)
                .title(title)
                .description(description)
                .thumbnailUrl(thumbnailUrl)
                .durationSeconds(durationSeconds)
                .publishedAt(publishedAt)
                .build();
    }

    private Optional<String> extractMediaId(String url, Pattern pattern) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        Matcher m = pattern.matcher(url);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String thumbnailUrl(JsonNode node) {
        JsonNode thumb = node.path("thumbnailUrl");
        if (thumb.isArray()) {
            return thumb.isEmpty() ? null : thumb.get(0).asText(null);
        }
        return thumb.isMissingNode() || thumb.isNull() ? null : thumb.asText();
    }

    private Integer parseDurationSeconds(String iso8601) {
        if (iso8601 == null) {
            return null;
        }
        try {
            return (int) Duration.parse(iso8601).toSeconds();
        } catch (DateTimeParseException e) {
            log.debug("Could not parse video duration '{}'", iso8601);
            return null;
        }
    }

    private Instant parseInstant(String isoDate) {
        if (isoDate == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(isoDate).toInstant();
        } catch (DateTimeParseException e1) {
            try {
                return Instant.parse(isoDate);
            } catch (DateTimeParseException e2) {
                log.debug("Could not parse video uploadDate '{}'", isoDate);
                return null;
            }
        }
    }
}
