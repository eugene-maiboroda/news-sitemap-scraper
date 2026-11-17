package com.article.adapter.discovery;

import com.article.adapter.http.HttpContentLoader;
import com.article.port.MenuDiscoveryPort;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "collector.site", havingValue = "AMERICAN_RIFLEMAN")
@RequiredArgsConstructor
public class AmericanRiflemanDiscoveryAdapter implements MenuDiscoveryPort {

    private static final String BASE_URL = "https://www.americanrifleman.org";
    private static final String PAGE_SIZE = "1000";
    private static final List<Section> SECTIONS = List.of(
            listing("/news/", "News"),
            new Section("/news/new-for-2026/", List.of(List.of("New For 2026"), List.of("News", "New For 2026")), true),
            listing("/news/ammunition/", "News", "Ammunition"),
            listing("/news/gear/", "News", "Gear"),
            listing("/news/guns/", "News", "Guns"),
            new Section("/news/historical/", List.of(List.of("Historical"), List.of("News", "Historical")), true),
            listing("/news/optics/", "News", "Optics"),
            new Section("/news/tips-techniques/", List.of(List.of("Tips & Techniques"), List.of("News", "Tips & Techniques")), true),
            listing("/news/new-for-2025/", "News", "New For 2025"),
            listing("/reviews/", "Reviews"),
            listing("/reviews/ammunition/", "Reviews", "Ammunition"),
            listing("/reviews/gear/", "Reviews", "Gear"),
            listing("/reviews/guns/", "Reviews", "Guns"),
            listing("/reviews/optics/", "Reviews", "Optics"),
            listing("/nra-gun-of-the-week/", "NRA Gun of the Week"),
            listing("/armed-citizen/", "Armed Citizen"),
            listing("/columns/", "Columns"),
            listing("/columns/political-report/", "Columns", "Political Report"),
            listing("/columns/president-s-column/", "Columns", "President's Column"),
            listing("/columns/standing-guard/", "Columns", "Standing Guard"),
            listing("/videos/", "Videos"),
            listing("/videos/artv/", "Videos", "ARTV"),
            listing("/videos/i-have-this-old-gun/", "Videos", "I Have This Old Gun"),
            listing("/videos/nra-gun-of-the-week/", "Videos", "NRA Gun Of The Week")
    );

    private final HttpContentLoader httpContentLoader;

    @Override
    public List<Section> sections() {
        return SECTIONS;
    }

    @Override
    public SectionResult discover(Section section) {
        if (!section.collectCards()) {
            return new SectionResult(List.of(), null);
        }

        Map<String, Card> collectedCards = new LinkedHashMap<>();
        Set<URI> visitedPages = new HashSet<>();
        Set<Set<String>> visitedPageArticleUrls = new HashSet<>();
        URI page = URI.create(BASE_URL + section.path());
        try {
            Element listing = loadListing(page);
            URI firstNextPage = nextPage(listing);
            if (firstNextPage != null) {
                page = firstBatchPage(firstNextPage, section);
                listing = loadListing(page);
            }

            visitedPages.add(page);
            while (true) {
                Map<String, Card> pageCards = readCards(listing);
                if (!visitedPageArticleUrls.add(Set.copyOf(pageCards.keySet()))) {
                    throw new IllegalStateException("Repeated set of listing cards");
                }
                pageCards.forEach(collectedCards::putIfAbsent);

                page = nextPage(listing);
                if (page == null) {
                    break;
                }
                validatePage(page, section);
                if (!visitedPages.add(page)) {
                    throw new IllegalStateException("Repeated listing URL");
                }
                listing = loadListing(page);
            }
            return new SectionResult(List.copyOf(collectedCards.values()), null);
        } catch (RuntimeException e) {
            return new SectionResult(List.copyOf(collectedCards.values()), section.path() + " [" + page + "]: " + e.getMessage());
        }
    }

    private static URI firstBatchPage(URI next, Section section) {
        validatePage(next, section);
        return UriComponentsBuilder.fromUri(next)
                .replaceQueryParam("pg", "1")
                .replaceQueryParam("it", PAGE_SIZE)
                .build(true)
                .toUri();
    }

    private Element loadListing(URI page) {
        String html = httpContentLoader.loadText(page.toASCIIString());
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("Empty HTML response");
        }
        Element listing = Jsoup.parse(html, page.toString()).selectFirst(".latest-stories-list");
        if (listing == null) {
            throw new IllegalStateException("Missing Latest Stories container");
        }
        return listing;
    }

    private Map<String, Card> readCards(Element listing) {
        Map<String, Card> cards = new LinkedHashMap<>();
        for (Element card : listing.select(".latest-story-copy")) {
            Element link = card.selectFirst("h3 a[href]");
            URI article = link == null ? null : parseLink(link);
            if (article == null || !isRiflemanUrl(article) || !article.getRawPath().startsWith("/content/")) {
                continue;
            }

            String url = articleUrl(article);
            Element description = card.selectFirst("p");
            cards.putIfAbsent(url, new Card(url, link.text(),
                    description == null ? null : description.text()));
        }
        return cards;
    }

    private static URI nextPage(Element listing) {
        Element next = listing.selectFirst("a.paging-next[href]");
        if (next == null) {
            return null;
        }
        URI uri = parseLink(next);
        if (uri == null) {
            throw new IllegalStateException("Invalid Next URL");
        }
        return uri;
    }

    private static URI parseLink(Element link) {
        String href = link.absUrl("href");
        try {
            return href.isBlank() ? null : URI.create(href).normalize();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void validatePage(URI uri, Section section) {
        if (!isRiflemanUrl(uri) || !section.path().equals(uri.getRawPath())) {
            throw new IllegalStateException("Unexpected listing URL " + uri);
        }
    }

    private static boolean isRiflemanUrl(URI uri) {
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && ("www.americanrifleman.org".equalsIgnoreCase(uri.getHost())
                        || "americanrifleman.org".equalsIgnoreCase(uri.getHost()))
                && uri.getUserInfo() == null
                && uri.getPort() == -1;
    }

    private static String articleUrl(URI article) {
        UriComponentsBuilder url = UriComponentsBuilder.fromUriString(BASE_URL + article.getRawPath())
                .query(article.getRawQuery());
        for (String name : url.build(true).getQueryParams().keySet()) {
            String key = name.toLowerCase(Locale.ROOT);
            if (key.startsWith("utm_") || Set.of("gclid", "fbclid", "msclkid").contains(key)) {
                url.replaceQueryParam(name);
            }
        }
        return url.build(true).toUriString();
    }

    private static Section listing(String path, String... hierarchy) {
        return new Section(path, List.of(List.of(hierarchy)), true);
    }
}
