package com.article.adapter.video;

import com.article.domain.Video;
import org.jsoup.nodes.Document;

import java.util.Optional;

/**
 * A single video provider (JW Player, Connatix, YouTube, ...). Implementations may support
 * several distinct embedding mechanisms for the same provider internally — that's an
 * implementation detail, not a reason for a separate type per mechanism.
 */
public interface VideoExtractor {

    Optional<Video> extract(Document document, String articleId, String articleUrl);
}
