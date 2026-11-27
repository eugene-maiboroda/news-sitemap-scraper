package com.article.adapter.video;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.video.decoder.MediaBinDecoder;
import com.article.application.PipelineRunSettings;
import com.article.domain.media.DownloadStatus;
import com.article.domain.media.Video;
import com.article.domain.media.VideoPlatform;
import com.article.port.out.BrowserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Connatix "bottom video" metadata. Unlike {@link JwVideoExtractor}, this cannot reuse
 * an already-fetched article {@link org.jsoup.nodes.Document} — it needs its own browser page load
 * (via {@link BrowserPort}) to intercept the {@code /core/pls} response, since the player is
 * initialized by JavaScript. After that, everything is plain HTTP (no browser involvement).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(30)
public class ConnatixVideoExtractor implements VideoExtractor {

    private static final String PLS_URL_CONTAINS = "capi.connatix.com/core/pls";
    private static final String CONNATIX_PLAYER_SELECTOR = "#connatix-outstream-video";
    private static final List<String> COOKIE_CONSENT_SELECTORS = List.of(
            "button:has-text('Allow All')",
            "button:has-text('Accept All')",
            "#onetrust-accept-btn-handler",
            ".fc-cta-consent"
    );
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final BrowserPort browserPort;
    private final HttpContentLoader httpContentLoader;
    private final PipelineRunSettings runSettings;

    @Override
    public Optional<Video> extract(Document document, String articleId, String articleUrl) {
        PipelineRunSettings.Snapshot settings = runSettings.current();
        if (!settings.extractConnatixVideos() || !settings.browser().enabled()) {
            return Optional.empty();
        }

        Optional<byte[]> plsBody = browserPort.interceptResponseBody(
                articleUrl, PLS_URL_CONTAINS, COOKIE_CONSENT_SELECTORS, CONNATIX_PLAYER_SELECTOR);
        if (plsBody.isEmpty()) {
            return Optional.empty();
        }

        Optional<String[]> ids = extractContentAndStreamId(plsBody.get());
        if (ids.isEmpty()) {
            log.debug("[CONNATIX] Could not extract contentId/streamId for {}", articleUrl);
            return Optional.empty();
        }
        String contentId = ids.get()[0];
        String streamId = ids.get()[1];

        byte[] mediaBin;
        try {
            String mediaBinUrl = "https://vid.connatix.com/%s/%s/2_media.bin".formatted(contentId, streamId);
            mediaBin = httpContentLoader.download(mediaBinUrl);
        } catch (Exception e) {
            log.warn("[CONNATIX] Failed to download 2_media.bin for {}: {}", articleUrl, e.getMessage());
            return Optional.empty();
        }

        String mmid = MediaBinDecoder.extractMmid(mediaBin);
        if (mmid == null) {
            log.debug("[CONNATIX] Could not extract mmid for {}", articleUrl);
            return Optional.empty();
        }

        String m3u8Url = "https://vid.connatix.com/%s/%s/mmid-%s/playlist.m3u8".formatted(contentId, streamId, mmid);

        return Optional.of(Video.builder()
                .articleId(articleId)
                .platform(VideoPlatform.CONNATIX_BOTTOM)
                .connatixContentId(contentId)
                .connatixStreamId(streamId)
                .connatixMmid(mmid)
                .m3u8Url(m3u8Url)
                .downloadStatus(DownloadStatus.PENDING)
                .build());
    }

    /**
     * The {@code /core/pls} protobuf response contains 5 UUIDs as ASCII text, in a fixed order;
     * index [2] is contentId and index [3] is streamId. Same crude-but-proven text-scan as the
     * verified Python reference — not a real protobuf field decode (unlike {@link MediaBinDecoder}).
     */
    Optional<String[]> extractContentAndStreamId(byte[] data) {
        String text = new String(data, StandardCharsets.ISO_8859_1);
        Matcher matcher = UUID_PATTERN.matcher(text);
        LinkedHashSet<String> uuids = new LinkedHashSet<>();
        while (matcher.find()) {
            uuids.add(matcher.group());
        }
        if (uuids.size() < 4) {
            return Optional.empty();
        }
        List<String> ordered = new ArrayList<>(uuids);
        return Optional.of(new String[]{ordered.get(2), ordered.get(3)});
    }
}
