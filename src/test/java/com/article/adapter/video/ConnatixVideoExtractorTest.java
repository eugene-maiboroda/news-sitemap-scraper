package com.article.adapter.video;

import com.article.adapter.http.HttpContentLoader;
import com.article.domain.status.DownloadStatus;
import com.article.domain.Video;
import com.article.domain.status.VideoPlatform;
import com.article.port.BrowserPort;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnatixVideoExtractorTest {

    private static final String ARTICLE_URL = "https://nypost.com/2026/06/26/some-article/";

    private final BrowserPort browserPort = mock(BrowserPort.class);
    private final HttpContentLoader httpContentLoader = mock(HttpContentLoader.class);
    private final ConnatixVideoExtractor extractor = new ConnatixVideoExtractor(browserPort, httpContentLoader);

    @Test
    void extractsContentAndStreamIdFromFiveUuids() {
        String contentId = UUID.randomUUID().toString();
        String streamId = UUID.randomUUID().toString();
        String text = "junk " + UUID.randomUUID() + " more "
                + UUID.randomUUID() + " then "
                + contentId + " and "
                + streamId + " and "
                + UUID.randomUUID() + " end";

        Optional<String[]> result = extractor.extractContentAndStreamId(text.getBytes(StandardCharsets.ISO_8859_1));

        assertTrue(result.isPresent());
        assertEquals(contentId, result.get()[0]);
        assertEquals(streamId, result.get()[1]);
    }

    @Test
    void returnsEmptyWhenFewerThanFourUuidsPresent() {
        String text = "only " + UUID.randomUUID() + " and " + UUID.randomUUID() + " here";

        Optional<String[]> result = extractor.extractContentAndStreamId(text.getBytes(StandardCharsets.ISO_8859_1));

        assertTrue(result.isEmpty());
    }

    @Test
    void extractsFullVideoWhenPlsAndMediaBinAvailable() {
        String contentId = UUID.randomUUID().toString();
        String streamId = UUID.randomUUID().toString();
        String mmid = UUID.randomUUID().toString();

        String plsText = "junk " + UUID.randomUUID() + " " + UUID.randomUUID() + " "
                + contentId + " " + streamId + " " + UUID.randomUUID() + " end";
        byte[] plsBody = plsText.getBytes(StandardCharsets.ISO_8859_1);

        when(browserPort.interceptResponseBody(anyString(), anyString(), any(), anyString()))
                .thenReturn(Optional.of(plsBody));
        when(httpContentLoader.download(anyString())).thenReturn(encodeField17(mmid));

        Optional<Video> result = extractor.extract("article-id-1", ARTICLE_URL);

        assertTrue(result.isPresent());
        Video video = result.get();
        assertEquals(VideoPlatform.CONNATIX_BOTTOM, video.getPlatform());
        assertEquals("article-id-1", video.getArticleId());
        assertEquals(contentId, video.getConnatixContentId());
        assertEquals(streamId, video.getConnatixStreamId());
        assertEquals(mmid, video.getConnatixMmid());
        assertEquals("https://vid.connatix.com/%s/%s/mmid-%s/playlist.m3u8".formatted(contentId, streamId, mmid), video.getM3u8Url());
        assertEquals(DownloadStatus.PENDING, video.getDownloadStatus());
        // exact endpoint per the verified Python reference — no "/api/v1/" prefix, that variant 403s
        verify(httpContentLoader).download("https://vid.connatix.com/%s/%s/2_media.bin".formatted(contentId, streamId));
    }

    @Test
    void returnsEmptyWhenNoPlsResponseIntercepted() {
        when(browserPort.interceptResponseBody(anyString(), anyString(), any(), anyString()))
                .thenReturn(Optional.empty());

        Optional<Video> result = extractor.extract("article-id-1", ARTICLE_URL);
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenMediaBinHasNoMmidField() {
        String contentId = UUID.randomUUID().toString();
        String streamId = UUID.randomUUID().toString();
        String plsText = "junk " + UUID.randomUUID() + " " + UUID.randomUUID() + " "
                + contentId + " " + streamId + " " + UUID.randomUUID() + " end";

        when(browserPort.interceptResponseBody(anyString(), anyString(), any(), anyString()))
                .thenReturn(Optional.of(plsText.getBytes(StandardCharsets.ISO_8859_1)));
        when(httpContentLoader.download(anyString())).thenReturn(new byte[0]);

        Optional<Video> result = extractor.extract("article-id-1", ARTICLE_URL);
        assertTrue(result.isEmpty());
    }

    /** Encodes a minimal protobuf message with field 17 (wire type LEN) set to the given string. */
    private static byte[] encodeField17(String value) {
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarint(out, (17 << 3) | 2);
        writeVarint(out, valueBytes.length);
        out.write(valueBytes, 0, valueBytes.length);
        return out.toByteArray();
    }

    private static void writeVarint(ByteArrayOutputStream out, int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.write(value);
                return;
            }
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }
}
