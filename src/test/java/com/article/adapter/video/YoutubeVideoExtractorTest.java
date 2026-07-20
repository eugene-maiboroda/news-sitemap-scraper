package com.article.adapter.video;

import com.article.domain.media.Video;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeVideoExtractorTest {

    private final YoutubeVideoExtractor extractor = new YoutubeVideoExtractor();
    private final String articleUrl =
            "https://nypost.com/2026/07/02/us-news/shocking-video-shows-army-veteran-being-fatally-shot-in-fight-over-walmart-parking-spot/";

    @Test
    void extractsVideoIdFromGutenbergYoutubeEmbed() {
        String html = """
                <html><body>
                <figure class="wp-block-embed is-type-video is-provider-youtube wp-block-embed-youtube wp-embed-aspect-16-9 wp-has-aspect-ratio">
                  <div class="wp-block-embed__wrapper">
                    <span class="embed-youtube" style="text-align:center; display: block;">
                      <iframe loading="lazy" title="Video" class="youtube-player" width="640" height="360"
                              src="https://www.youtube.com/embed/1h6zr9iFnEA?version=3&rel=1&showsearch=0&showinfo=1&iv_load_policy=1&fs=1&hl=en-US&autohide=2&wmode=transparent"
                              allowfullscreen="true" style="border:0;"></iframe>
                    </span>
                  </div>
                </figure>
                </body></html>
                """;
        Document doc = Jsoup.parse(html, articleUrl);

        Optional<Video> result = extractor.extract(doc, "abc123456", articleUrl);

        assertTrue(result.isPresent());
        Video video = result.get();
        assertEquals("1h6zr9iFnEA", video.youtubeVideoId());
        assertEquals("https://img.youtube.com/vi/1h6zr9iFnEA/hqdefault.jpg", video.thumbnailUrl());
        assertEquals("abc123456", video.articleId());
    }

    @Test
    void returnsEmptyWhenNoYoutubeEmbedPresent() {
        Document doc = Jsoup.parse("<html><body><p>No video here</p></body></html>", articleUrl);

        assertTrue(extractor.extract(doc, "abc123456", articleUrl).isEmpty());
    }
}
