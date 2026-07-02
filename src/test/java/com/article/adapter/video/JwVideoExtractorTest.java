package com.article.adapter.video;

import com.article.domain.Video;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwVideoExtractorTest {

    private final JwVideoExtractor extractor = new JwVideoExtractor(new ObjectMapper());

    @Test
    void extractsJwMediaIdAndMetadataFromJsonLd() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "VideoObject",
                    "name": "Test Video Title",
                    "description": "Test video description",
                    "thumbnailUrl": ["https://example.com/thumb.jpg"],
                    "uploadDate": "2026-06-26T10:00:00-04:00",
                    "duration": "PT2M35S",
                    "embedUrl": "https://content.jwplatform.com/players/x6zqbaOP-SPdDOYds.html",
                    "contentUrl": "https://cdn.jwplayer.com/videos/x6zqbaOP-RyIcpnTz.mp4"
                }
                </script>
                </head><body></body></html>
                """;
        Document doc = Jsoup.parse(html, "https://nypost.com/2026/06/26/some-article/");

        Optional<Video> result = extractor.extract(doc, "abc123456");

        assertTrue(result.isPresent());
        Video video = result.get();
        assertEquals("x6zqbaOP", video.jwMediaId());
        assertEquals("https://cdn.jwplayer.com/manifests/x6zqbaOP.m3u8", video.m3u8Url());
        assertEquals("Test Video Title", video.title());
        assertEquals("Test video description", video.description());
        assertEquals("https://example.com/thumb.jpg", video.thumbnailUrl());
        assertEquals(155, video.durationSeconds());
        assertEquals(OffsetDateTime.parse("2026-06-26T10:00:00-04:00").toInstant(), video.publishedAt());
        assertEquals("abc123456", video.articleId());
    }

    @Test
    void returnsEmptyWhenNoVideoObjectPresent() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {"@context": "https://schema.org", "@type": "NewsArticle", "headline": "No video here"}
                </script>
                </head><body></body></html>
                """;
        Document doc = Jsoup.parse(html, "https://nypost.com/2026/06/26/no-video/");

        Optional<Video> result = extractor.extract(doc, "def987654");

        assertTrue(result.isEmpty());
    }
}
