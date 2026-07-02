package com.article.adapter.video;

import com.article.domain.DownloadStatus;
import com.article.domain.Video;
import com.article.domain.VideoPlatform;
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
 * Extracts JW Player "top video" metadata from the schema.org VideoObject JSON-LD
 * already embedded in a NYPost article page. Uses the same {@link Document} the
 * article parser already fetched — no extra HTTP request to the article page.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwVideoExtractor {

    private static final Pattern MANIFEST_ID = Pattern.compile("cdn\\.jwplayer\\.com/manifests/([A-Za-z0-9]{8})\\.m3u8");
    private static final Pattern EMBED_ID = Pattern.compile("jwplatform\\.com/players/([A-Za-z0-9]{8})-");
    private static final Pattern CONTENT_ID = Pattern.compile("cdn\\.jwplayer\\.com/videos/([A-Za-z0-9]{8})-");

    private final ObjectMapper objectMapper;

    public Optional<Video> extract(Document doc, String articleId) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            JsonNode videoObject = findVideoObject(script.data());
            if (videoObject != null) {
                Optional<Video> video = toVideo(videoObject, articleId);
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

    private Optional<Video> toVideo(JsonNode videoNode, String articleId) {
        String embedUrl = videoNode.path("embedUrl").asText("");
        String contentUrl = videoNode.path("contentUrl").asText("");

        String mediaId = extractMediaId(contentUrl, MANIFEST_ID)
                .or(() -> extractMediaId(embedUrl, EMBED_ID))
                .or(() -> extractMediaId(contentUrl, CONTENT_ID))
                .orElse(null);

        if (mediaId == null) {
            return Optional.empty();
        }

        return Optional.of(Video.builder()
                .articleId(articleId)
                .platform(VideoPlatform.JW_TOP)
                .jwMediaId(mediaId)
                .m3u8Url("https://cdn.jwplayer.com/manifests/" + mediaId + ".m3u8")
                .downloadStatus(DownloadStatus.PENDING)
                .title(textOrNull(videoNode, "name"))
                .description(textOrNull(videoNode, "description"))
                .thumbnailUrl(thumbnailUrl(videoNode))
                .durationSeconds(parseDurationSeconds(textOrNull(videoNode, "duration")))
                .publishedAt(parseInstant(textOrNull(videoNode, "uploadDate")))
                .build());
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
