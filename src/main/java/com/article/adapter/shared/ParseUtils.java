package com.article.adapter.shared;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtils {

    public static final String EMPTY = "";
    private static final Pattern SRCSET_SPLIT = Pattern.compile("\\s*,\\s*(?=https?://)");
    private static final Pattern WIDTH_PATTERN = Pattern.compile("w_(\\d+)");

    public static String getText(Element element) {
        return element.text().trim();
    }

    public static String getText(Elements elements) {
        return elements != null ? elements.text().trim() : EMPTY;
    }

    public static String getImageSrc(Elements elements) {
        for (Element el : elements) {
            if (el.hasAttr("src") && !el.attr("src").isBlank()) {
                return el.absUrl("src");
            }
            if (el.hasAttr("data-src")) {
                return el.absUrl("data-src");
            }
        }
        return EMPTY;
    }

    public static String getFirstLink(Elements elements) {
        List<String> links = getLinks(elements);
        return links.isEmpty() ? EMPTY : links.get(0);
    }

    public static List<String> getLinks(Elements elements) {
        List<String> links = new ArrayList<>();
        for (Element element : elements) {
            String href = element.attr("abs:href");
            if (href.isBlank()) {
                href = element.attr("href");
            }
            if (!href.isBlank()) {
                links.add(href);
            }
        }
        return links;
    }

    public static String extractBestImage(Elements elements) {
        List<ImageCandidate> candidates = new ArrayList<>();
        for (Element source : elements) {
            String srcset = source.attr("srcset");
            if (srcset.isBlank()) continue;
            for (String part : SRCSET_SPLIT.split(srcset.trim())) {
                String url = part.trim().split("\\s+")[0];
                Integer width = extractWidth(url);
                if (width != null) {
                    candidates.add(new ImageCandidate(url, width));
                }
            }
        }
        return candidates.stream()
                .max(Comparator.comparingInt(ImageCandidate::width))
                .map(ImageCandidate::url)
                .orElse(EMPTY);
    }

    public static String imageFilePrefix(String category, String id) {
        String prefix = (category == null || category.isBlank()) ? "unknown" :
                category.toLowerCase().replaceAll("\\s+", "_");
        return prefix + "_" + id;
    }

    private static Integer extractWidth(String url) {
        Matcher m = WIDTH_PATTERN.matcher(url);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private record ImageCandidate(String url, int width) {}
}
