package com.article.adapter.export.resolver;

import lombok.Getter;

import java.util.List;

/**
 * Fixed Sports Betting export targets. These are not derived from stored topic data because
 * Sports Betting pages don't carry enough navigation
 * information to reconstruct a hierarchy from an article's topics, so the URL/hierarchy/required
 * tag are known constants instead. Membership is decided from stored tags: an article matches a
 * target only if it carries both the generic "Sports Betting" tag and this target's league-specific
 * tag.
 */
@Getter
public enum SportsBettingExportTarget {

    NFL("https://nypost.com/nfl-betting/", List.of("Sports Betting", "NFL"), "NFL Betting", "nfl_betting.json"),
    NBA("https://nypost.com/nba-betting/", List.of("Sports Betting", "NBA"), "NBA Betting", "nba_betting.json"),
    MLB("https://nypost.com/mlb-betting/", List.of("Sports Betting", "MLB"), "MLB Betting", "mlb_betting.json"),
    NHL("https://nypost.com/nhl-betting/", List.of("Sports Betting", "NHL"), "NHL Betting", "nhl_betting.json");

    private final String topicUrl;
    private final List<String> hierarchy;
    private final String requiredTag;
    private final String fileName;

    SportsBettingExportTarget(String topicUrl, List<String> hierarchy, String requiredTag, String fileName) {
        this.topicUrl = topicUrl;
        this.hierarchy = hierarchy;
        this.requiredTag = requiredTag;
        this.fileName = fileName;
    }
}
