package com.article.adapter.article;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.ParseUtils;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;
import com.article.port.ArticlePagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleParser implements ArticlePagePort {

    private static final String TITLE_SELECTOR = ".relative h1";
    private static final String SUB_TITLE_SELECTOR = ".relative .article-intro";
    private static final String AUTHOR_SELECTOR = ".underline";
    private static final String IMAGE_SELECTOR = "#main-article div:has(.underline) + div source";
    private static final String ALT_IMAGE_SELECTOR = "#main-article div:has(.underline) + div figure img";
    private static final String CAPTION_SELECTOR = "#main-article div:has(.underline) + div div.items-center";
    private static final String DATE_SELECTOR = "#main-article div:has(.underline) time";
    private static final String CONTENT_SELECTOR = ".article-content";

    private final HttpContentLoader contentLoader;

    @Override
    public ParseResult parse(ArticleLink link) {
        String url = link.articleUrl();
        try {
            Document doc = contentLoader.loadDocument(url);

            Article article = Article.builder()
                    .id(idFromUrl(url))
                    .category(null)
                    .url(url)
                    .publishDate(parsePublishDate(doc))
                    .title(ParseUtils.getText(doc.select(TITLE_SELECTOR)))
                    .subTitle(ParseUtils.getText(doc.select(SUB_TITLE_SELECTOR)))
                    .author(ParseUtils.getText(doc.select(AUTHOR_SELECTOR).eq(0)))
                    .authorLink(ParseUtils.getFirstLink(doc.select(AUTHOR_SELECTOR).eq(0)))
                    .content(doc.select(CONTENT_SELECTOR).html())
                    .imageUrl(resolveImage(doc))
                    .caption(ParseUtils.getText(doc.select(CAPTION_SELECTOR)))
                    .build();

            return ParseResult.ok(article);

        } catch (HttpStatusCodeException e) {
            log.error("[PARSE] HTTP {} for {}", e.getStatusCode().value(), url);
            return ParseResult.error(url, "HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            log.error("[PARSE] Failed to fetch {}: {}", url, e.getMessage());
            return ParseResult.error(url, e.getMessage());
        }
    }

    private LocalDate parsePublishDate(Document doc) {
        String datetime = doc.select(DATE_SELECTOR).attr("datetime");
        return Instant.parse(datetime).atZone(ZoneId.of("America/New_York")).toLocalDate();
    }

    private String resolveImage(Document doc) {
        Elements sourceTags = doc.select(IMAGE_SELECTOR);
        String image = ParseUtils.extractBestImage(sourceTags);
        if (!image.isEmpty()) {
            return image;
        }
        String fallback = ParseUtils.getImageSrc(doc.select(ALT_IMAGE_SELECTOR));
        if (!fallback.isEmpty()) {
            return fallback;
        }
        sleepBriefly();
        image = ParseUtils.extractBestImage(doc.select(IMAGE_SELECTOR));
        return image.isEmpty() ? ParseUtils.getImageSrc(doc.select(ALT_IMAGE_SELECTOR)) : image;
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String idFromUrl(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 9);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
