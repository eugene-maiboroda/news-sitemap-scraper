package com.article.adapter.discovery;

import com.article.adapter.http.HttpContentLoader;
import com.article.port.MenuDiscoveryPort.Section;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "collector.site", havingValue = "NRAILA")
@RequiredArgsConstructor
public class NrailaSectionCollector {

    static final String BASE_URL = "https://www.nraila.org";
    static final String STATES_PATH = "/gun-laws/state-gun-laws/";
    static final String ALERTS_PATH = "/grassroots-alerts/";
    static final String ISSUE_PATH = ALERTS_PATH + "grassroots-alert/";

    private final HttpContentLoader httpContentLoader;
    private final Map<String, String> catalogErrors = new LinkedHashMap<>();

    public List<Section> collect(List<Section> menu) {
        catalogErrors.clear();
        Map<String, Section> sections = new LinkedHashMap<>();
        for (Section section : menu) {
            sections.put(section.path(), section);
            if (STATES_PATH.equals(section.path()) || ALERTS_PATH.equals(section.path())) {
                collectSections(section, sections);
            }
        }
        return List.copyOf(sections.values());
    }

    public String error(Section section) {
        return catalogErrors.get(section.path());
    }

    private void collectSections(Section catalog, Map<String, Section> sections) {
        URI page = URI.create(BASE_URL + catalog.path());
        Set<URI> visited = new HashSet<>();
        Set<Set<String>> pageUrls = new HashSet<>();
        try {
            while (page != null) {
                validatePage(page, catalog);
                if (!visited.add(page)) {
                    throw new IllegalStateException("Repeated catalog URL " + page);
                }
                Document document = loadDocument(page);
                Map<String, Element> entries = readEntries(document);
                if (!entries.isEmpty() && !pageUrls.add(Set.copyOf(entries.keySet()))) {
                    throw new IllegalStateException("Repeated catalog entries " + page);
                }
                for (Map.Entry<String, Element> entry : entries.entrySet()) {
                    URI url = URI.create(entry.getKey());
                    boolean state = STATES_PATH.equals(catalog.path());
                    boolean valid = isSiteUrl(url) && (state
                            ? url.getPath().startsWith(STATES_PATH) && !url.getPath().equals(STATES_PATH)
                            : ISSUE_PATH.equals(url.getPath()) && query(url).getFirst("id") != null);
                    if (!valid) {
                        throw new IllegalStateException("Unexpected catalog entry " + url);
                    }
                    String name = state ? entry.getValue().text().replaceFirst(" Gun Laws$", "") : entry.getValue().text();
                    List<String> hierarchy = new ArrayList<>(catalog.hierarchies().getFirst());
                    hierarchy.add(name);
                    String path = url.getRawPath();
                    if (url.getRawQuery() != null) {
                        path += "?" + url.getRawQuery();
                    }
                    sections.putIfAbsent(path, new Section(path, List.of(List.copyOf(hierarchy)), true));
                }
                URI next = nextPage(document);
                if (entries.isEmpty() && (next != null || visited.size() == 1)) {
                    throw new IllegalStateException("Empty catalog without confirmed end " + page);
                }
                validateNext(page, next, catalog);
                page = next;
            }
        } catch (RuntimeException e) {
            catalogErrors.put(catalog.path(), page + ": " + e.getMessage());
            sections.put(catalog.path(), new Section(catalog.path(), catalog.hierarchies(), true));
        }
    }

    private static Map<String, Element> readEntries(Document document) {
        Element listing = document.selectFirst(".news-items");
        if (listing == null) {
            throw new IllegalStateException("Missing main news-items container");
        }
        Map<String, Element> entries = new LinkedHashMap<>();
        for (Element item : listing.select(".news-item")) {
            Element link = item.selectFirst(".news-article-headline a[href]");
            if (link == null || link.text().isBlank()) {
                throw new IllegalStateException("Missing card headline");
            }
            entries.putIfAbsent(linkUrl(link).toASCIIString(), link);
        }
        return entries;
    }

    Document loadDocument(URI page) {
        String html = httpContentLoader.loadText(page.toASCIIString());
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("Empty HTML response " + page);
        }
        return Jsoup.parse(html, page.toString());
    }

    static URI nextPage(Document document) {
        Element next = document.selectFirst(".page-nav a[href]:has(.page-nav-next)");
        return next == null ? null : linkUrl(next);
    }

    static MultiValueMap<String, String> query(URI uri) {
        return UriComponentsBuilder.fromUri(uri).build(true).getQueryParams();
    }

    static URI linkUrl(Element link) {
        try {
            URI uri = URI.create(link.absUrl("href")).normalize();
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Not an HTTP link");
            }
            String address = uri.toASCIIString();
            int fragment = address.indexOf('#');
            if (fragment >= 0) {
                address = address.substring(0, fragment);
            }
            if (isSiteUrl(uri)) {
                address = BASE_URL + uri.getRawPath();
                if (uri.getRawQuery() != null) {
                    address += "?" + uri.getRawQuery();
                }
            }
            return URI.create(address);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid link " + link.attr("href"), e);
        }
    }

    static void validatePage(URI page, Section section) {
        URI source = URI.create(BASE_URL + section.path());
        if (!isSiteUrl(page) || !source.getRawPath().equals(page.getRawPath())
                || !Objects.equals(query(source).getFirst("id"), query(page).getFirst("id"))) {
            throw new IllegalStateException("Unexpected page URL " + page);
        }
    }

    static void validateNext(URI current, URI next, Section section) {
        if (next == null) {
            return;
        }
        validatePage(next, section);
        MultiValueMap<String, String> filters = query(current);
        for (String name : filters.keySet()) {
            if (!name.equals("page") && !Objects.equals(filters.get(name), query(next).get(name))) {
                throw new IllegalStateException("Next changed filter " + name + ": " + next);
            }
        }
    }

    static boolean isSiteUrl(URI uri) {
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && ("www.nraila.org".equalsIgnoreCase(uri.getHost()) || "nraila.org".equalsIgnoreCase(uri.getHost()))
                && uri.getPort() == -1 && uri.getUserInfo() == null;
    }

}
