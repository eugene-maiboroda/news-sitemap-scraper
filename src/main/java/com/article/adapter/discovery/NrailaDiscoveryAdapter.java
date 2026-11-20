package com.article.adapter.discovery;

import com.article.port.MenuDiscoveryPort;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.article.adapter.discovery.NrailaSectionCollector.BASE_URL;
import static com.article.adapter.discovery.NrailaSectionCollector.STATES_PATH;
import static com.article.adapter.discovery.NrailaSectionCollector.ALERTS_PATH;
import static com.article.adapter.discovery.NrailaSectionCollector.ISSUE_PATH;
import static com.article.adapter.discovery.NrailaSectionCollector.query;
import static com.article.adapter.discovery.NrailaSectionCollector.linkUrl;
import static com.article.adapter.discovery.NrailaSectionCollector.nextPage;
import static com.article.adapter.discovery.NrailaSectionCollector.validatePage;
import static com.article.adapter.discovery.NrailaSectionCollector.validateNext;
import static com.article.adapter.discovery.NrailaSectionCollector.isSiteUrl;

@Component
@ConditionalOnProperty(name = "collector.site", havingValue = "NRAILA")
@RequiredArgsConstructor
public class NrailaDiscoveryAdapter implements MenuDiscoveryPort {

    private static final int PAGE_LIMIT = 50;
    private static final DateTimeFormatter FILTER_DATE = DateTimeFormatter.ofPattern("uuuuMMdd");
    private static final DateTimeFormatter CARD_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d, uuuu", Locale.US);
    private static final Set<String> SINGLE_PAGES = Set.of(
            "/legacy/",
            "/why-gun-control-doesn-t-work/",
            "/what-is-the-second-amendment-and-how-is-it-defined/",
            "/legal-legislation/current-litigation/"
    );
    private static final List<Section> SECTIONS = List.of(
            listing("/news/", "News"),
            listing("/news/articles/", "News", "Articles"),
            listing("/news/in-the-news/", "News", "In the News"),
            listing("/news/nra-ila-press-releases/", "News", "Press Releases"),
            listing("/news/from-the-director/", "News", "From the Director"),
            new Section("/gun-laws/armed-citizen/",
                    List.of(List.of("News", "Armed Citizen®"), List.of("Gun Laws", "Armed Citizen®")), true),
            new Section("/get-the-facts/", List.of(List.of("Get the Facts")), false),
            listing("/get-the-facts/ammunition/", "Get the Facts", "Ammunition"),
            listing("/get-the-facts/assault-weapons-large-magazines/",
                    "Get the Facts", "\"Assault Weapons\" | \"Large\" Magazines"),
            listing("/get-the-facts/background-checks-nics/", "Get the Facts", "Background Checks | NICS"),
            listing("/get-the-facts/what-is-the-charleston-loophole/",
                    "Get the Facts", "What is the Charleston \"Loophole\""),
            listing("/get-the-facts/batfefederal-firearms-law-reform/",
                    "Get the Facts", "BATFE/Federal Firearms Law Reform"),
            listing("/get-the-facts/crime-criminal-justice/", "Get the Facts", "Crime | Criminal Justice"),
            listing("/get-the-facts/emergency-powers-law/", "Get the Facts", "Emergency Powers Law"),
            listing("/get-the-facts/waiting-periods/", "Get the Facts", "Waiting Periods"),
            listing("/get-the-facts/gun-safety/", "Get the Facts", "Gun Safety"),
            listing("/get-the-facts/firearm-transportation/", "Get the Facts", "Firearm Transportation"),
            listing("/get-the-facts/gun-shows/", "Get the Facts", "Gun Shows"),
            listing("/get-the-facts/handguns/", "Get the Facts", "Handguns"),
            listing("/get-the-facts/internationalun-gun-control-issues/",
                    "Get the Facts", "International & United Nations Gun Control"),
            listing("/get-the-facts/micro-stamping-and-ballistic-fingerprinting/",
                    "Get the Facts", "Micro-Stamping | Ballistic \"Fingerprinting\""),
            listing("/get-the-facts/national-firearms-act-nfa/", "Get the Facts", "National Firearms Act (NFA)"),
            listing("/get-the-facts/preemption-laws/", "Get the Facts", "Firearm Preemption Laws"),
            listing("/get-the-facts/registration-licensing/", "Get the Facts", "Gun Registration | Gun Licensing"),
            listing("/get-the-facts/right-to-carry-and-concealed-carry/",
                    "Get the Facts", "Concealed Carry | Right-to-Carry"),
            listing("/get-the-facts/second-amendment-right-to-keep-and-bear-arms/",
                    "Get the Facts", "Second Amendment & Right to Keep and Bear Arms"),
            listing("/get-the-facts/smart-gunspersonalized-firearms/",
                    "Get the Facts", "\"Smart\" Guns | Personalized Firearms"),
            listing("/get-the-facts/gun-violence-research/", "Get the Facts", "Public Health Care Bias"),
            listing("/get-the-facts/terrorist-watchlistno-fly-list/",
                    "Get the Facts", "Terrorist Watchlist | No-Fly List"),
            listing("/gun-laws/", "Gun Laws"),
            new Section(STATES_PATH, List.of(List.of("Gun Laws", "State Gun Laws")), false),
            listing("/gun-laws/leosa/", "Gun Laws", "LEOSA"),
            listing("/legal-legislation/", "Legal & Legislation"),
            listing("/legal-legislation/federal-legislation/", "Legal & Legislation", "Federal Legislation"),
            listing("/legal-legislation/state-legislation/", "Legal & Legislation", "State Legislation"),
            listing("/legal-legislation/legal-articles/", "Legal & Legislation", "Legal Articles"),
            listing("/legal-legislation/current-litigation/", "Legal & Legislation", "Current Litigation"),
            listing("/campaigns/huntingconservation/hunting-home-page/", "Hunting"),
            listing("/hunting/alerts/", "Hunting", "Alerts"),
            listing("/hunting/articles/", "Hunting", "Articles"),
            listing("/hunting/news/", "Hunting", "News"),
            new Section(ALERTS_PATH, List.of(List.of("Grassroots Alerts")), false),
            listing("/legacy/", "Leave a Legacy"),
            listing("/why-gun-control-doesn-t-work/", "Why Gun Control Doesn’t Work"),
            listing("/what-is-the-second-amendment-and-how-is-it-defined/",
                    "What Is The Second Amendment And How Is It Defined"),
            listing("/grassroots/become-an-nra-campaign-field-representative-today/",
                    "Become an NRA-ILA Campaign Field Rep Today!")
    );

    private final NrailaSectionCollector sectionCollector;

    @Override
    public List<Section> sections() {
        return sectionCollector.collect(SECTIONS);
    }

    @Override
    public SectionResult discover(Section section) {
        String catalogError = sectionCollector.error(section);
        if (catalogError != null) {
            return new SectionResult(List.of(), catalogError);
        }
        if (!section.collectCards()) {
            return new SectionResult(List.of(), null);
        }

        Map<String, Card> cards = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        if (SINGLE_PAGES.contains(section.path())) {
            cards.put(BASE_URL + section.path(), new Card(BASE_URL + section.path(), sectionTitle(section), null));
        } else if (section.path().startsWith(ISSUE_PATH)) {
            collectIssue(section, cards, errors);
        } else {
            collectInterval(section, LocalDate.of(1, 1, 1), LocalDate.now(ZoneId.of("Europe/Madrid")), cards, errors);
        }
        return new SectionResult(List.copyOf(cards.values()), errors.isEmpty() ? null : String.join("; ", errors));
    }

    private void collectInterval(Section section, LocalDate start, LocalDate end,
                                 Map<String, Card> cards, List<String> errors) {
        URI page = intervalPage(URI.create(BASE_URL + section.path()), start, end);
        Set<URI> visited = new HashSet<>();
        Set<Set<String>> pageUrls = new HashSet<>();
        boolean split = false;
        try {
            while (page != null) {
                validatePage(page, section);
                if (!visited.add(page)) {
                    throw new IllegalStateException("Repeated listing URL");
                }
                Document document = sectionCollector.loadDocument(page);
                Map<String, Card> entries = readCards(document);
                validateDates(document, start, end);
                if (!entries.isEmpty() && !pageUrls.add(Set.copyOf(entries.keySet()))) {
                    throw new IllegalStateException("Repeated set of listing cards");
                }
                entries.forEach(cards::putIfAbsent);
                URI next = nextPage(document);
                if (entries.isEmpty()) {
                    if (next != null) {
                        throw new IllegalStateException("Empty listing with Next");
                    }
                    break;
                }
                if (visited.size() >= PAGE_LIMIT) {
                    if (start.equals(end)) {
                        throw new IllegalStateException("One day reached the 50-page limit");
                    }
                    split = true;
                    break;
                }
                validateNext(page, next, section);
                page = next == null ? null : intervalPage(next, start, end);
            }
        } catch (RuntimeException e) {
            errors.add(section.path() + " [" + start + ".." + end + "] " + page + ": " + e.getMessage());
        }
        if (split) {
            LocalDate middle = start.plusDays((end.toEpochDay() - start.toEpochDay()) / 2);
            collectInterval(section, start, middle, cards, errors);
            collectInterval(section, middle.plusDays(1), end, cards, errors);
        }
    }

    private void collectIssue(Section section, Map<String, Card> cards, List<String> errors) {
        URI page = URI.create(BASE_URL + section.path());
        Set<URI> visited = new HashSet<>();
        Set<String> visitedContent = new HashSet<>();
        try {
            while (page != null) {
                validatePage(page, section);
                if (!visited.add(page)) {
                    throw new IllegalStateException("Repeated issue URL");
                }
                Document document = sectionCollector.loadDocument(page);
                Element content = document.selectFirst(".article-content");
                if (content == null || content.text().isBlank()) {
                    throw new IllegalStateException("Missing issue content");
                }
                if (!visitedContent.add(content.html())) {
                    throw new IllegalStateException("Repeated issue content " + page);
                }
                Map<String, Card> pageCards = new LinkedHashMap<>();
                for (Element link : content.select("a[href]")) {
                    String title = link.text();
                    if (title.isBlank()) {
                        continue;
                    }
                    String href = link.absUrl("href");
                    if (!href.startsWith("http://") && !href.startsWith("https://")) {
                        continue;
                    }
                    URI url = linkUrl(link);
                    boolean article = isSiteUrl(url) && (url.getPath().startsWith("/articles/")
                            || url.getPath().endsWith(".aspx")
                            && (url.getPath().startsWith("/legislation/")
                            || url.getPath().startsWith("/news-issues/articles/")));
                    boolean heading = link.closest("h1, h2, h3") != null;
                    if (!article && !heading) {
                        continue;
                    }
                    if (title.toUpperCase(Locale.ROOT).startsWith("READ MORE")) {
                        Element headingElement = link.parent();
                        title = headingElement == null ? null : headingElement.text();
                    }
                    String address = url.toASCIIString();
                    pageCards.putIfAbsent(address, new Card(address, title, null));
                }
                boolean selfMaterial = pageCards.isEmpty() || hasUnlinkedMaterial(content);
                pageCards.forEach(cards::putIfAbsent);
                if (selfMaterial) {
                    String address = BASE_URL + section.path();
                    cards.putIfAbsent(address, new Card(address, sectionTitle(section), null));
                }
                URI next = nextPage(document);
                validateNext(page, next, section);
                page = next;
            }
        } catch (RuntimeException e) {
            errors.add(page + ": " + e.getMessage());
        }
    }

    private static boolean hasUnlinkedMaterial(Element content) {
        for (Element heading : content.select("h1, h2, h3")) {
            if (!heading.text().isBlank() && heading.selectFirst("a[href]") == null) {
                Element next = heading.nextElementSibling();
                if (next != null && next.normalName().equals("p") && !next.text().isBlank()
                        && next.selectFirst("a[href]") == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, Card> readCards(Document document) {
        Element listing = document.selectFirst(".news-items");
        if (listing == null) {
            throw new IllegalStateException("Missing main news-items container");
        }
        Map<String, Card> cards = new LinkedHashMap<>();
        for (Element item : listing.select(".news-item")) {
            Element link = item.selectFirst(".news-article-headline a[href]");
            if (link == null || link.text().isBlank()) {
                throw new IllegalStateException("Missing card headline");
            }
            String address = linkUrl(link).toASCIIString();
            Element description = link.parent().parent().selectFirst("p:not(.news-date):not(.news-state)");
            cards.putIfAbsent(address, new Card(address, link.text(), description == null ? null : description.text()));
        }
        return cards;
    }

    private static void validateDates(Document document, LocalDate start, LocalDate end) {
        for (Element item : document.select(".news-items .news-item")) {
            Element date = item.selectFirst(".news-date");
            if (date == null) {
                throw new IllegalStateException("Missing listing date");
            }
            LocalDate published = LocalDate.parse(date.text(), CARD_DATE);
            if (published.isBefore(start) || published.isAfter(end)) {
                throw new IllegalStateException("Listing date outside requested interval: " + published);
            }
        }
    }

    private static URI intervalPage(URI uri, LocalDate start, LocalDate end) {
        for (String name : List.of("startDate", "endDate")) {
            String expected = FILTER_DATE.format(name.equals("startDate") ? start : end);
            String actual = query(uri).getFirst(name);
            if (actual != null && !actual.equals(expected)) {
                throw new IllegalStateException("Next changed " + name + ": " + uri);
            }
        }
        return UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam("startDate", FILTER_DATE.format(start))
                .replaceQueryParam("endDate", FILTER_DATE.format(end))
                .build(true)
                .toUri();
    }

    private static String sectionTitle(Section section) {
        return section.hierarchies().getFirst().getLast();
    }

    private static Section listing(String path, String... hierarchy) {
        return new Section(path, List.of(List.of(hierarchy)), true);
    }
}
