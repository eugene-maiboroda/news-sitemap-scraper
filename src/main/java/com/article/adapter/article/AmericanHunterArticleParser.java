package com.article.adapter.article;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.ArticleIds;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.domain.Image;
import com.article.domain.NamedLink;
import com.article.domain.ParseResult;
import com.article.port.ArticlePagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "collector.site", havingValue = "AMERICAN_HUNTER", matchIfMissing = true)
public class AmericanHunterArticleParser implements ArticlePagePort {

    private static final DateTimeFormatter DATE_IN_BYLINE = DateTimeFormatter
            .ofPattern("MMMM d, uuuu", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern BYLINE_DATE = Pattern.compile("posted on\\s+([A-Za-z]+ \\d{1,2}, \\d{4})");
    private static final Pattern YOUTUBE_EMBED = Pattern.compile(
            "^https?://(?:www\\.)?youtube(?:-nocookie)?\\.com/embed/([\\w-]{11})(?:[?#].*)?$");

    private final HttpContentLoader loader;

    @Override
    public ParseResult parse(ArticleLink link) {
        String url = link.articleUrl();
        String html;
        try {
            html = loader.loadText(url);
        } catch (RuntimeException e) {
            Throwable cause = e instanceof RestClientException ? e : e.getCause();
            if (cause instanceof RestClientResponseException response) {
                return ParseResult.error(url, "Article HTTP " + response.getStatusCode().value());
            }
            if (cause instanceof RestClientException) {
                return ParseResult.error(url, "Article HTTP request failed: " + cause.getClass().getSimpleName());
            }
            throw e;
        }
        return parseHtml(url, html);
    }

    ParseResult parseHtml(String url, String html) {
        if (html == null || html.isBlank()) {
            return ParseResult.error(url, "Empty article response");
        }

        Document document = Jsoup.parse(html, url);
        String title = text(document.selectFirst("main .page-headline h1"));
        Element subTitle = document.selectFirst("main .page-headline h1 + h2");
        Element scope = document.selectFirst("main .component.article-content");
        if (title == null || scope == null) {
            return ParseResult.error(url, "Missing article title or container");
        }

        String id = ArticleIds.fromUrl(url);
        String bodyHtml = articleContent(scope);
        List<Image> images = images(scope, id);
        List<String> videoUrls = videoUrls(scope);

        if (scope.select(".article-rte").html().isBlank() && images.isEmpty() && videoUrls.isEmpty()) {
            return ParseResult.error(url, "Missing article body and media");
        }

        Element author = scope.selectFirst(".article-attribution .article-author");
        Element authorLink = author == null ? null : author.selectFirst("a[href]");
        String authorUrl = authorLink == null ? "" : authorLink.absUrl("href");
        Element publication = document.selectFirst("meta[name=publicationdate]");
        LocalDate date = publishedDate(publication == null ? "" : publication.attr("content"),
                scope.selectFirst(".article-attribution"));
        if (date == null) {
            log.warn("[HUNTER] Missing publication date: {}", url);
        }

        Article article = Article.builder()
                .id(id)
                .url(url)
                .title(title)
                .subtitles(text(subTitle))
                .publishDate(date)
                .author(text(author))
                .authorLink(authorUrl.isBlank() ? null : authorUrl)
                .content(bodyHtml)
                .build();

        String rawHtml = scope.outerHtml();
        String affiliateDisclosure = text(scope.selectFirst(
                ".col-12.text-center:containsOwn(When you buy products through the links on our site)"));
        List<NamedLink> tags = links(scope, ".article-attribution .tag a[href]");
        List<NamedLink> footerTags = links(scope, ".in-this-article .tag a[href]");
        List<NamedLink> relatedStories = links(scope, ".related-title + ul a[href]");

        return new ParseResult.MenuArticleSuccess(
                article,
                rawHtml,
                affiliateDisclosure,
                tags,
                footerTags,
                relatedStories,
                images,
                videoUrls
        );
    }

    private static String articleContent(Element scope) {
        Element content = scope.clone();
        for (Element sidebar : content.select(".row > .col-lg-4.col-xl-3")) {
            if (sidebar.siblingElements().stream()
                    .anyMatch(column -> column.hasClass("col-lg-8") && column.hasClass("col-xl-9"))) {
                sidebar.remove();
            }
        }
        return content.html();
    }

    @Override
    public ArchivedContent parseArchivedContent(String articleId, String articleUrl, String rawHtml) {
        Element scope = Jsoup.parse(rawHtml, articleUrl).selectFirst(".component.article-content");
        if (scope == null || scope.selectFirst(".article-rte, .video-responsive, figure") == null) {
            throw new IllegalArgumentException("Missing archived article content for " + articleUrl);
        }
        return new ArchivedContent(articleContent(scope), images(scope, articleId));
    }

    private static String originalImageUrl(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (("www.americanhunter.org".equalsIgnoreCase(host) || "americanhunter.org".equalsIgnoreCase(host))
                && uri.getPath().startsWith("/media/")) {
            return url.split("[?#]", 2)[0];
        }
        return url;
    }

    private static List<String> videoUrls(Element scope) {
        return scope.select(
                        ".video-responsive iframe, .article-rte iframe, .article-rte video, .video-responsive video")
                .stream()
                .map(AmericanHunterArticleParser::videoUrl)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public List<Image> parseArchivedImages(String articleId, String articleUrl, String rawHtml) {
        Element scope = Jsoup.parse(rawHtml, articleUrl).selectFirst(".component.article-content");
        return scope == null ? List.of() : images(scope, articleId, false);
    }

    private static List<Image> images(Element scope, String articleId) {
        return images(scope, articleId, true);
    }

    private static List<Image> images(Element scope, String articleId, boolean useOriginalUrl) {
        List<Image> images = new ArrayList<>();
        for (Element image : scope.select(".col-xl-9 > figure img, .article-rte img")) {
            String url = mediaUrl(image);
            if (url.isBlank()) {
                continue;
            }
            images.add(Image.builder()
                    .articleId(articleId)
                    .imageUrl(useOriginalUrl ? originalImageUrl(url) : url)
                    .position(images.size())
                    .caption(imageCaption(image))
                    .build());
        }
        return images;
    }

    private static String imageCaption(Element image) {
        Element figure = image.closest("figure");
        if (figure != null && figure.select("img").size() == 1) {
            String caption = text(figure.selectFirst("figcaption"));
            if (caption != null) {
                return caption;
            }
        }

        Element imageParagraph = image.closest("p");
        if (imageParagraph == null || imageParagraph.select("img").size() != 1) {
            return null;
        }

        Element inlineCaption = image.nextElementSibling();
        if (image.parent() == imageParagraph && imageParagraph.childrenSize() == 2
                && imageParagraph.ownText().isBlank() && inlineCaption != null
                && inlineCaption.normalName().equals("strong")) {
            return text(inlineCaption);
        }
        if (!imageParagraph.text().isBlank()) {
            return null;
        }

        Element captionParagraph = imageParagraph.nextElementSibling();
        if (captionParagraph == null || !captionParagraph.normalName().equals("p")
                || captionParagraph.children().isEmpty() || !captionParagraph.ownText().isBlank()
                || !captionParagraph.select("img, a, span").isEmpty()) {
            return null;
        }

        for (Element child : captionParagraph.children()) {
            if (!child.normalName().equals("strong")) {
                return null;
            }
        }
        return text(captionParagraph);
    }

    private static List<NamedLink> links(Element scope, String selector) {
        return scope.select(selector).stream()
                .map(link -> new NamedLink(link.text(), link.absUrl("href")))
                .filter(link -> !link.name().isBlank() && !link.url().isBlank())
                .distinct()
                .toList();
    }

    private static String videoUrl(Element element) {
        String url = mediaUrl(element);
        if ("video".equals(element.normalName())) {
            Element source = element.selectFirst("source[src]");
            return url.isBlank() && source != null ? source.absUrl("src") : url;
        }

        Matcher match = YOUTUBE_EMBED.matcher(url);
        if (match.matches()) {
            return "https://www.youtube.com/watch?v=" + match.group(1);
        }
        try {
            URI source = URI.create(url);
            String host = source.getHost();
            String path = source.getPath();
            if (host == null || path == null) {
                return "";
            }
            if (host.equalsIgnoreCase("www.facebook.com") || host.equalsIgnoreCase("facebook.com")) {
                return path.equals("/plugins/video.php") ? url : "";
            }
            if (host.equalsIgnoreCase("player.vimeo.com") || element.closest(".video-responsive") != null) {
                return url;
            }
        } catch (IllegalArgumentException ignored) {
            // Skip malformed iframe URLs.
        }
        return "";
    }

    private static String mediaUrl(Element element) {
        String lazyUrl = element.absUrl("data-src");
        return lazyUrl.isBlank() ? element.absUrl("src") : lazyUrl;
    }

    private static LocalDate publishedDate(String metadata, Element attribution) {
        if (metadata.length() >= 10) {
            try {
                return LocalDate.parse(metadata.substring(0, 10));
            } catch (DateTimeParseException ignored) {
                // Try the date shown on the page.
            }
        }
        if (attribution != null) {
            Matcher match = BYLINE_DATE.matcher(attribution.text());
            if (match.find()) {
                try {
                    return LocalDate.parse(match.group(1), DATE_IN_BYLINE);
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String text(Element element) {
        return element == null || element.text().isBlank() ? null : element.text();
    }
}
