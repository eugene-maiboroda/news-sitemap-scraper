package com.article.adapter.shared;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class FileNaming {

    private FileNaming() {
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

    public static String avatarFilePrefix(String authorId) {
        return "author_" + authorId + "_avatar";
    }

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

    public static List<String> buildHierarchy(String mainCategory, List<String> topicNames) {
        List<String> hierarchy = new ArrayList<>();
        addIfNewAndNotBlank(hierarchy, mainCategory);
        topicNames.forEach(name -> addIfNewAndNotBlank(hierarchy, name));
        return List.copyOf(hierarchy);
    }

    public static String shortHash(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static void addIfNewAndNotBlank(List<String> hierarchy, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (hierarchy.isEmpty() || !hierarchy.getLast().equals(name)) {
            hierarchy.add(name);
        }
    }
}
