package com.article.adapter.article;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.ParseUtils;
import com.article.adapter.video.VideoExtractor;
import com.article.domain.article.Article;
import com.article.domain.article.ArticleLink;
import com.article.domain.media.DownloadStatus;
import com.article.domain.media.Image;
import com.article.application.dto.ParseResult;
import com.article.domain.media.Video;
import com.article.port.out.ArticleParserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.article.adapter.shared.ParseUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleParser implements ArticleParserPort {

    private static final String CATEGORY_SELECTOR = ".article-header__top span";
    private static final String TITLE_SELECTOR = ".article-header h1";
    private static final String CONTENT_SELECTOR = ".entry-content";
    private static final String ARTICLE_SELECTOR = "article";

    private static final String IMAGES_SELECTOR = ".entry-content img, .featured-image img";
    private static final String ALT_CONTENT_IMAGES_SELECTOR = "figure img";
    private static final String AUTHOR_SELECTOR = ".article-header .byline__author";

    private final HttpContentLoader contentLoader;
    private final List<VideoExtractor> videoExtractors;
    private final TopicsTagsExtractor topicsTagsExtractor;

    @Override
    public ParseResult parse(ArticleLink link) {
        String url = link.articleUrl();
        String html = contentLoader.loadText(url);
        Document doc = Jsoup.parse(html, url);
        Element articleEl = doc.select(ARTICLE_SELECTOR).first();
        clearMarketing(articleEl);
        String articleId = idFromUrl(url);

        String title = getText(doc.select(TITLE_SELECTOR));
        String content = doc.select(CONTENT_SELECTOR).html();

        if (title.isBlank()) {
            log.warn("[PARSE] Missing title: {}", url);
            return ParseResult.error(url, "Missing title");
        }

        if (content.isBlank()) {
            log.warn("[PARSE] Missing content: {}", url);
            return ParseResult.error(url, "Missing content");
        }

        Article article = Article.builder()
                .id(articleId)
                .category(getText(doc.select(CATEGORY_SELECTOR)))
                .url(url)
                .publishDate(link.articleDate())
                .title(title)
                .subTitle(EMPTY)
                .author(getText(doc.select(AUTHOR_SELECTOR).eq(0)))
                .authorLink(getFirstLink(doc.select(AUTHOR_SELECTOR).eq(0)))
                .content(content)
                .build();

        List<Image> images = extractImages(doc, articleId);

        List<Video> videos = videoExtractors.stream()
                .map(extractor -> extractor.extract(doc, articleId, url))
                .flatMap(Optional::stream)
                .toList();

        return ParseResult.ok(article, topicsTagsExtractor.extract(doc), images, videos);
    }

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
        List<Element> candidates = doc.select(selector).stream()
                .filter(ParseUtils::isLikelyContentImage)
                .toList();
        return dedupByWpImageId(candidates);
    }

    private List<Element> dedupByWpImageId(List<Element> candidates) {
        List<Element> deduped = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Element img : candidates) {
            Optional<String> id = ParseUtils.wpImageId(img);
            if (id.isEmpty() || seenIds.add(id.get())) {
                deduped.add(img);
            }
        }
        return deduped;
    }

    private String extractCaption(Element img) {
        Element figure = img.closest("figure");
        if (figure == null) {
            return null;
        }
        Element figcaption = figure.selectFirst("figcaption");
        return figcaption != null ? getText(figcaption) : null;
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
