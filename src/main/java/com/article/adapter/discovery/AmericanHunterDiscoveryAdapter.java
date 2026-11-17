package com.article.adapter.discovery;

import com.article.adapter.http.HttpContentLoader;
import com.article.port.MenuDiscoveryPort;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
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
@RequiredArgsConstructor
public class AmericanHunterDiscoveryAdapter implements MenuDiscoveryPort {

    private static final String BASE_URL = "https://www.americanhunter.org";
    private static final String PAGE_SIZE = "20000";
    private static final List<Section> SECTIONS = List.of(
            listing("/news/", "News"),
            listing("/new-for-2026/", "New for 2026"),
            listing("/sundaygunday/", "#SundayGunday"),
            listing("/game/", "Game"),
            listing("/guns-gear/", "Guns & Gear"),
            listing("/guns-gear/reviews/", "Guns & Gear", "Reviews"),
            listing("/guns-gear/head-to-head/", "Guns & Gear", "Head to Head"),
            listing("/guns-gear/rifles/", "Guns & Gear", "Rifles"),
            listing("/guns-gear/pistols/", "Guns & Gear", "Pistols"),
            listing("/guns-gear/shotguns/", "Guns & Gear", "Shotguns"),
            listing("/guns-gear/muzzleloaders/", "Guns & Gear", "Muzzleloaders"),
            listing("/guns-gear/ammo/", "Guns & Gear", "Ammo"),
            listing("/guns-gear/optics/", "Guns & Gear", "Optics"),
            new Section("/guns-gear/archery/",
                    List.of(List.of("Archery"), List.of("Guns & Gear", "Archery")), true),
            listing("/guns-gear/gear/", "Guns & Gear", "Gear"),
            listing("/tips-tactics/", "Tips & Tactics"),
            listing("/join-the-hunt/", "Join the Hunt"),
            listing("/lifestyle/", "Lifestyle"),
            listing("/wild-game-recipes/", "Wild Game Recipes"),
            listing("/videos/", "Videos"),
            listing("/videos/african-experience/", "Videos", "African Experience"),
            listing("/videos/american-hunter-films/", "Videos", "American Hunter Films"),
            listing("/videos/remington-model-700-american-hunter-rifle-project/",
                    "Videos", "Remington Model 700 American Hunter Rifle Project"),
            new Section("/state-fish-game-information/",
                    List.of(List.of("State Fish & Game Information")), false)
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
            if (article == null || !isHunterUrl(article) || !article.getRawPath().startsWith("/content/")) {
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
        if (!isHunterUrl(uri) || !section.path().equals(uri.getRawPath())) {
            throw new IllegalStateException("Unexpected listing URL " + uri);
        }
    }

    private static boolean isHunterUrl(URI uri) {
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && ("www.americanhunter.org".equalsIgnoreCase(uri.getHost())
                        || "americanhunter.org".equalsIgnoreCase(uri.getHost()))
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
