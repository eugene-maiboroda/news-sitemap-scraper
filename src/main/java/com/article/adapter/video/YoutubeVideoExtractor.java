package com.article.adapter.video;

import com.article.domain.status.DownloadStatus;
import com.article.domain.Video;
import com.article.domain.status.VideoPlatform;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts YouTube video embeds straight from the article's already-fetched {@link Document} —
 * no extra HTTP request, no browser. NYPost embeds YouTube videos via WordPress core's Gutenberg
 * embed block ({@code figure.is-provider-youtube}, added by WP core regardless of theme), which
 * server-renders the {@code <iframe>} directly, so plain HTML parsing is sufficient to find it.
 * Only the video id is extractable this way; title/description/duration/publishedAt are absent
 * from the HTML and are left for a later metadata step (oEmbed/API/yt-dlp).
 */
@Component
public class YoutubeVideoExtractor implements VideoExtractor {

    private static final String YOUTUBE_EMBED_SELECTOR = "figure.is-provider-youtube iframe";
    private static final Pattern VIDEO_ID = Pattern.compile("youtube\\.com/embed/([A-Za-z0-9_-]+)");

    @Override
    public Optional<Video> extract(Document document, String articleId, String articleUrl) {
        for (Element iframe : document.select(YOUTUBE_EMBED_SELECTOR)) {
            Matcher m = VIDEO_ID.matcher(iframe.attr("src"));
            if (m.find()) {
                return Optional.of(buildVideo(articleId, m.group(1)));
            }
        }
        return Optional.empty();
    }

    private Video buildVideo(String articleId, String videoId) {
        return Video.builder()
                .articleId(articleId)
                .platform(VideoPlatform.YOUTUBE)
                .youtubeVideoId(videoId)
                .thumbnailUrl("https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg")
                .downloadStatus(DownloadStatus.PENDING)
                .build();
    }
}
