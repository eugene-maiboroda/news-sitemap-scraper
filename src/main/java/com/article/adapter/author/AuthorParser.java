package com.article.adapter.author;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.ParseUtils;
import com.article.domain.Author;
import com.article.domain.AuthorParseResult;
import com.article.port.AuthorParserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorParser implements AuthorParserPort {

    private static final String AVATAR_SELECTOR = ".archive__header img";
    // .author-bio is the single root wrapper across every real layout observed (verified against
    // 160+ live author pages): sometimes it holds only .author-lede__wrapper (a short lede),
    // sometimes only .author-bio__wrapper (a "Background" heading + paragraph), and sometimes both
    // as siblings (e.g. https://nypost.com/author/andrew-crane/). The actual bio text itself is not
    // reliably wrapped in <p> — some pages have it as a bare text node directly inside the wrapper
    // div (e.g. https://nypost.com/author/jeff-landry/) — so selecting "p" specifically silently
    // drops those. Taking the whole container's text and stripping only heading elements (the
    // "Background" label) covers every real layout without a two-step fallback and without ever
    // duplicating text.
    private static final String BIO_SELECTOR = ".author-bio";
    private static final String HEADING_SELECTOR = "h1, h2, h3, h4, h5, h6";

    private final HttpContentLoader contentLoader;

    @Override
    public AuthorParseResult parse(Author author) {
        String url = author.authorUrl();
        try {
            String html = contentLoader.loadText(url);
            if (html == null || html.isBlank()) {
                log.warn("[PARSE][author] Empty response: {}", url);
                return AuthorParseResult.error(url, "Empty response");
            }
            Document doc = Jsoup.parse(html, url);

            // bio/avatar are optional per-author — plenty of real author pages carry neither, so a
            // missing selector match is not a parse failure, only a genuine load/HTTP/parse problem is.
            String bio = extractBio(doc);
            String avatarUrl = extractAvatarUrl(doc);

            Author updated = new Author(
                    author.id(),
                    author.authorName(),
                    author.authorUrl(),
                    bio,
                    avatarUrl,
                    author.avatarFileId());

            return AuthorParseResult.ok(updated);
        } catch (HttpStatusCodeException e) {
            log.error("[PARSE][author] HTTP {} for {}", e.getStatusCode().value(), url);
            return AuthorParseResult.error(url, "HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            log.error("[PARSE][author] Failed to fetch {}: {}", url, e.getMessage());
            return AuthorParseResult.error(url, e.getMessage());
        }
    }

    private String extractBio(Document doc) {
        Element bio = doc.selectFirst(BIO_SELECTOR);
        if (bio == null) {
            return null;
        }
        Element withoutHeadings = bio.clone();
        withoutHeadings.select(HEADING_SELECTOR).remove();
        String text = withoutHeadings.text().trim();
        return text.isBlank() ? null : text;
    }

    // Same per-image approach as ArticleParser.extractImages(): pick the widest srcset candidate,
    // falling back to the plain src/data-src when no srcset is present. The size gate in
    // ParseUtils.isLikelyContentImage() is not applicable here — this img's own width/height
    // attributes are the small rendered avatar size (e.g. 76x69), not the real photo resolution,
    // which only shows up among the srcset candidates.
    private String extractAvatarUrl(Document doc) {
        Element avatar = doc.select(AVATAR_SELECTOR).first();
        if (avatar == null) {
            return null;
        }
        Elements single = new Elements(avatar);
        String imageUrl = ParseUtils.extractBestImage(single);
        if (imageUrl.isEmpty()) {
            imageUrl = ParseUtils.getImageSrc(single);
        }
        return imageUrl.isEmpty() ? null : imageUrl;
    }
}