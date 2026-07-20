package com.article.adapter.article;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
@Component
public class MainCategoryResolver {

    private static final Map<String, String> MAIN_CATEGORY_BY_TOPIC = buildMapping();


    private static final List<UrlRule> URL_RULES = List.of(
            new UrlRule(url -> url != null && url.contains("/sports-plus/"), "Post Sports+")
    );

    public String resolve(String topic1Name, String topic1Url) {
        for (UrlRule rule : URL_RULES) {
            if (rule.matches(topic1Url)) {
                return rule.category();
            }
        }
        if (topic1Name == null) {
            return null;
        }
        String mainCategory = MAIN_CATEGORY_BY_TOPIC.get(normalize(topic1Name));
        if (mainCategory == null) {
            log.debug("Unknown topic '{}' — main category not resolved", topic1Name);
        }
        return mainCategory;
    }

    private static String normalize(String value) {
        return value.trim().replace('’', '\'').toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> buildMapping() {
        Map<String, String> map = new HashMap<>();
        put(map, "Sports", "NBA", "NFL", "MLB", "Olympics", "NHL",
                "College Football", "College Basketball", "WNBA");
        put(map, "Sports Betting", "Betting");
        put(map, "Entertainment", "TV", "Movies", "Music", "Celebrities", "Awards", "Theater");
        put(map, "Lifestyle", "Weird But True", "Sex & Relationships", "Viral Trends",
                "Human Interest", "Parenting", "Fashion & Beauty", "Food & Drink", "Travel");
        put(map, "Health", "Wellness", "Fitness", "Health Care", "Medicine",
                "Men's Health", "Women's Health", "Mental Health", "Nutrition");
        put(map, "Science", "Space", "Environment", "Wildlife", "Archaeology");
        put(map, "US News", "Metro", "Long Island", "Politics");
        put(map, "US News", "US News");
        put(map, "World News", "World News");
        put(map, "Page Six", "Page Six");
        put(map, "Sports", "Sports");
        put(map, "Post Sports+", "Post Sports+");
        put(map, "Sports Betting", "Sports Betting");
        put(map, "Business", "Business");
        put(map, "Opinion", "Opinion");
        put(map, "Entertainment", "Entertainment");
        put(map, "Shopping", "Shopping");
        put(map, "Lifestyle", "Lifestyle");
        put(map, "Health", "Health");
        put(map, "Real Estate", "Real Estate");
        put(map, "Alexa", "Alexa");
        put(map, "Media", "Media");
        put(map, "Tech", "Tech");
        put(map, "Science", "Science");
        put(map, "Astrology", "Astrology");
        put(map, "Video", "Video");
        put(map, "Photos", "Photos");
        put(map, "Pod Force One", "Pod Force One");
        put(map, "NY POSTcast", "NY POSTcast");
        put(map, "Sports", "Sports Entertainment", "Golf", "Soccer", "Tennis",
                "Boxing", "Wrestling", "MMA", "NWSL", "PWHL", "Sports Video");
        put(map, "Entertainment", "Ticket Sales");
        put(map, "Real Estate", "Celebrity Real Estate", "Commercial", "Residential");
        put(map, "Lifestyle", "Jobs", "Pets", "Home", "Cars");
        put(map, "Contributor Content", "Contributor Content");
        put(map, "Games", "Crosswords & Games", "Daily Sudoku");
        return Map.copyOf(map);
    }

    private static void put(Map<String, String> map, String mainCategory, String... topics) {
        for (String topic : topics) {
            map.put(normalize(topic), mainCategory);
        }
    }

    private record UrlRule(Predicate<String> matcher, String category) {
        boolean matches(String url) {
            return matcher.test(url);
        }
    }
}
