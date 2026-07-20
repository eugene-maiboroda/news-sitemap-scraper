package com.article.adapter.shared;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtils {

    public static final String EMPTY = "";
    private static final Pattern SRCSET_SPLIT = Pattern.compile("\\s*,\\s*(?=https?://)");
    // matches the HTML5 srcset width descriptor itself (e.g. "1024w"), not the URL —
    // URL query schemes vary per site/CDN, but the descriptor format is a standard.
    private static final Pattern WIDTH_DESCRIPTOR_PATTERN = Pattern.compile("(\\d+)w");
    // WordPress core's own marker for "this <img> came from the media library, inserted into
    // post content" — added automatically regardless of theme, unlike template-generated
    // thumbnails/widgets (related posts, shop promos, columnist avatars), which never carry it.
    private static final Pattern WP_IMAGE_ID = Pattern.compile("wp-image-(\\d+)");

    private static final int MIN_CONTENT_IMAGE_WIDTH = 300;
    private static final int MIN_CONTENT_IMAGE_HEIGHT = 200;

    public static String getText(Element element) {
        return element.text().trim();
    }

    public static String getText(Elements elements) {
        return elements != null ? elements.text().trim() : EMPTY;
    }

    public static void clearMarketing(Element el) {
        if (el == null || el.html().isBlank()) {
            return;
        }
        el.select("script").remove();
        el.select("style").remove();
        el.select("noscript").remove();

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
        return links.isEmpty() ? EMPTY : links.getFirst();
    }

    public static List<String> getLinks(Elements elements) {
        List<String> links = new ArrayList<>();
        for (Element element : elements) {
            if ("a".equals(element.tagName())) {
                addLink(element, links);
                continue;
            }

            for (Element link : element.select("a[href]")) {
                addLink(link, links);
            }
        }

        return links;
    }

    private static void addLink(Element element, List<String> links) {
        String href = element.absUrl("href");
        if (href.isBlank()) {
            href = element.attr("href");
        }

        if (!href.isBlank()) {
            links.add(href);
        }
    }

    public static String extractBestImage(Elements elements) {
        List<ImageCandidate> candidates = new ArrayList<>();
        for (Element source : elements) {
            candidates.addAll(parseSrcsetCandidates(source));
        }
        return candidates.stream()
                .max(Comparator.comparingInt(ImageCandidate::width))
                .map(ImageCandidate::url)
                .orElse(EMPTY);
    }

    /**
     * Universal heuristic for "is this a real article photo, not decorative/promo/related-post chrome" —
     * no site-specific string checks (e.g. URL/class-name matching against one site's branding):
     * <ol>
     *   <li>large enough to plausibly be a real photo rather than a thumbnail/icon/banner — using the
     *       image's own declared {@code width}/{@code height} attributes when present, else the widest
     *       {@code srcset} candidate as a proxy; checked unconditionally, since neither signal below is
     *       reliable on its own for small promo/badge images;</li>
     *   <li>then either: WordPress core's own {@code wp-image-<id>} class (added to any content image
     *       inserted from the media library, regardless of theme), or</li>
     *   <li>wrapped in a {@code <figure>} — the standard convention (classic-editor captions and
     *       Gutenberg's image block both use it) for an inserted content image without that class.</li>
     * </ol>
     */
    public static boolean isLikelyContentImage(Element img) {
        if (!meetsMinimumSize(img)) {
            return false;
        }
        if (hasWpImageClass(img)) {
            return true;
        }
        return img.closest("figure") != null;
    }

    /** The numeric WordPress media-library attachment id, if this image carries a {@code wp-image-<id>} class. */
    public static Optional<String> wpImageId(Element img) {
        Matcher m = WP_IMAGE_ID.matcher(img.attr("class"));
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    public static String categorySlug(String category) {
        return (category == null || category.isBlank())
                ? "uncategorized"
                : category.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    public static String imageFilePrefix(String category, String articleId, Integer position) {
        return categorySlug(category) + "_" + articleId + "_img_" + position;
    }
    public static String videoFilePrefix(String category, String articleId, String platform, String videoId) {
        return categorySlug(category) + "_" + articleId + "_video_" + platform.toLowerCase() + "_" + videoId;
    }

    /** Deterministic id for an author's avatar file, keyed off the stable author id — reused as-is
     *  by the future avatar-download step, so a file saved from this prefix always matches the
     *  avatarId already written into the author export JSON. */
    public static String avatarFilePrefix(String authorId) {
        return "author_" + authorId + "_avatar";
    }

    /** File extension inferred from a media URL's path (ignoring query params), defaulting to
     *  {@code .jpg} when none is present — shared by every download adapter that saves a URL's
     *  bytes under a deterministic *FilePrefix name (see ImageDownloadAdapter, avatar download). */
    public static String resolveExtension(String url) {
        int question = url.indexOf('?');
        String clean = question == -1 ? url : url.substring(0, question);

        int dot = clean.lastIndexOf('.');
        return dot == -1 ? ".jpg" : clean.substring(dot);
    }

    public static String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "unknown";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    /**
     * Builds a topic-file display hierarchy from a main category and an ordered list of topic
     * names, skipping null/blank entries and collapsing an entry that repeats the immediately
     * preceding one (e.g. main category equals the first topic name), and doing the same for any
     * adjacent repeat deeper in the hierarchy.
     */
    public static List<String> buildHierarchy(String mainCategory, List<String> topicNames) {
        List<String> hierarchy = new ArrayList<>();
        addIfNewAndNotBlank(hierarchy, mainCategory);
        for (String name : topicNames) {
            addIfNewAndNotBlank(hierarchy, name);
        }
        return List.copyOf(hierarchy);
    }

    private static void addIfNewAndNotBlank(List<String> hierarchy, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (!hierarchy.isEmpty() && hierarchy.get(hierarchy.size() - 1).equals(name)) {
            return;
        }
        hierarchy.add(name);
    }

    /** SHA-256 based, not {@code String.hashCode()} — must stay stable across JVM runs/versions. */
    public static String shortHash(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean hasWpImageClass(Element img) {
        return wpImageId(img).isPresent();
    }

    private static boolean meetsMinimumSize(Element img) {
        Integer width = parsePositiveInt(img.attr("width"));
        Integer height = parsePositiveInt(img.attr("height"));
        if (width != null && height != null) {
            return width >= MIN_CONTENT_IMAGE_WIDTH && height >= MIN_CONTENT_IMAGE_HEIGHT;
        }
        Integer widestSrcsetWidth = widestSrcsetWidth(img);
        return widestSrcsetWidth != null && widestSrcsetWidth >= MIN_CONTENT_IMAGE_WIDTH;
    }

    private static Integer widestSrcsetWidth(Element img) {
        return parseSrcsetCandidates(img).stream()
                .map(ImageCandidate::width)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static List<ImageCandidate> parseSrcsetCandidates(Element element) {
        List<ImageCandidate> candidates = new ArrayList<>();
        String srcset = element.attr("srcset");
        if (srcset.isBlank()) {
            return candidates;
        }
        for (String part : SRCSET_SPLIT.split(srcset.trim())) {
            String[] tokens = part.trim().split("\\s+");
            if (tokens.length < 2) continue;
            Integer width = extractWidth(tokens[tokens.length - 1]);
            if (width != null) {
                candidates.add(new ImageCandidate(tokens[0], width));
            }
        }
        return candidates;
    }

    private static Integer extractWidth(String descriptor) {
        Matcher m = WIDTH_DESCRIPTOR_PATTERN.matcher(descriptor);
        return m.matches() ? Integer.parseInt(m.group(1)) : null;
    }

    private static Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }



    private record ImageCandidate(String url, int width) {}
}
