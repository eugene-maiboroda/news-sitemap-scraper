package com.article.adapter.article;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.ParseUtils;
import com.article.adapter.video.ConnatixVideoExtractor;
import com.article.adapter.video.JwVideoExtractor;
import com.article.bootstrap.CollectorProps;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.domain.DownloadStatus;
import com.article.domain.Image;
import com.article.domain.ParseResult;
import com.article.domain.Video;
import com.article.port.ArticlePagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleParser implements ArticlePagePort {

    private static final String CATEGORY_SELECTOR = ".article-header__top span";
    private static final String TITLE_SELECTOR = ".article-header h1";
    private static final String SUB_TITLE_SELECTOR = "";
    private static final String AUTHOR_SELECTOR = ".article-header .byline__author";
    private static final String CONTENT_SELECTOR = ".entry-content";
//    private static final String CONTENT_SELECTOR = "single__content";

    private static final String IMAGES_SELECTOR = ".single__content.entry-content img";
    private static final String ALT_CONTENT_IMAGES_SELECTOR = "figure img";

    //    private static final String DATE_SELECTOR = "#main-article div:has(.underline) time";
    private final HttpContentLoader contentLoader;
    private final JwVideoExtractor jwVideoExtractor;
    private final ConnatixVideoExtractor connatixVideoExtractor;
    private final CollectorProps collectorProps;

    @Override
    public ParseResult parse(ArticleLink link) {
        String url = link.articleUrl();
        try {
           String html = contentLoader.loadText(url);
            Document doc = Jsoup.parse(html, url);
            String articleId = idFromUrl(url);

            Article article = Article.builder()
                    .id(articleId)
                    .category(ParseUtils.getText(doc.select(CATEGORY_SELECTOR)))
                    .url(url)
                    .publishDate(link.articleDate())
                    .title(ParseUtils.getText(doc.select(TITLE_SELECTOR)))
                    .subTitle("Test")
                    .author(ParseUtils.getText(doc.select(AUTHOR_SELECTOR).eq(0)))
                    .authorLink(ParseUtils.getFirstLink(doc.select(AUTHOR_SELECTOR).eq(0)))
                    .content(doc.select(CONTENT_SELECTOR).html())
                    .rawHtml(html)
                    .build();

            List<Image> images = extractImages(doc, articleId);

            List<Video> videos = new ArrayList<>();
            jwVideoExtractor.extract(doc, articleId, url).ifPresent(videos::add);
            if (collectorProps.steps().extractConnatixVideos()) {
                connatixVideoExtractor.extract(articleId, url).ifPresent(videos::add);
            }

            return ParseResult.ok(article, images, videos);

        } catch (HttpStatusCodeException e) {
            log.error("[PARSE] HTTP {} for {}", e.getStatusCode().value(), url);
            return ParseResult.error(url, "HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            log.error("[PARSE] Failed to fetch {}: {}", url, e.getMessage());
            return ParseResult.error(url, e.getMessage());
        }
    }

//    private LocalDate parsePublishDate(Document doc) {
//        String datetime = doc.select(DATE_SELECTOR).attr("datetime");
//        return Instant.parse(datetime).atZone(ZoneId.of("America/New_York")).toLocalDate();
//    }

    private List<Image> extractImages(Document doc, String articleId) {
        List<Element> imgElements = contentImages(doc, IMAGES_SELECTOR);
        if (imgElements.isEmpty()) {
            imgElements = contentImages(doc, ALT_CONTENT_IMAGES_SELECTOR);
        }

        List<Image> images = new ArrayList<>();
        int position = 0;
        for (Element img : imgElements) {
            Elements single = new Elements(img);
            String imageUrl = ParseUtils.extractBestImage(single);
            if (imageUrl.isEmpty()) {
                imageUrl = ParseUtils.getImageSrc(single);
            }
            if (imageUrl.isEmpty()) {
                log.warn("[PARSE] No image found for {}", img);
                continue;
            }
            images.add(Image.builder()
                    .articleId(articleId)
                    .imageUrl(imageUrl)
                    .caption(extractCaption(img))
                    .position(position++)
                    .downloadStatus(DownloadStatus.PENDING)
                    .build());
        }
        return images;
    }

    private List<Element> contentImages(Document doc, String selector) {
        return doc.select(selector).stream()
                .filter(ParseUtils::isLikelyContentImage)
                .toList();
    }

    private String extractCaption(Element img) {
        Element figure = img.closest("figure");
        if (figure == null) {
            return null;
        }
        Element figcaption = figure.selectFirst("figcaption");
        return figcaption != null ? ParseUtils.getText(figcaption) : null;
    }

    static String idFromUrl(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(url.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
