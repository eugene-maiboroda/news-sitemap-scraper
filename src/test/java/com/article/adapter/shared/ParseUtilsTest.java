package com.article.adapter.shared;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ParseUtilsTest {

    @Test
    void buildsTwoLevelHierarchy() {
        List<String> hierarchy = ParseUtils.buildHierarchy("Sports", Arrays.asList("NFL"));

        assertEquals(List.of("Sports", "NFL"), hierarchy);
    }

    @Test
    void dedupesMainCategoryEqualToTopic1() {
        List<String> hierarchy = ParseUtils.buildHierarchy("Sports", Arrays.asList("Sports", "NFL"));

        assertEquals(List.of("Sports", "NFL"), hierarchy);
    }

    @Test
    void skipsNullOnIntermediatePosition() {
        // Post Sports+ / topic1=null / topic2=Inside the Knicks
        List<String> hierarchy = ParseUtils.buildHierarchy("Post Sports+", Arrays.asList(null, "Inside the Knicks"));

        assertEquals(List.of("Post Sports+", "Inside the Knicks"), hierarchy);
    }

    @Test
    void mainCategoryOnlyWhenNoTopicsResolved() {
        List<String> hierarchy = ParseUtils.buildHierarchy("Business", Arrays.asList((String) null));

        assertEquals(List.of("Business"), hierarchy);
    }

    @Test
    void shortHashIsDeterministicAcrossCalls() {
        String first = ParseUtils.shortHash("https://nypost.com/new-york-liberty/");
        String second = ParseUtils.shortHash("https://nypost.com/new-york-liberty/");

        assertEquals(first, second);
    }

    @Test
    void shortHashDiffersForDifferentInput() {
        String a = ParseUtils.shortHash("https://nypost.com/new-york-liberty/#Sports#WNBA");
        String b = ParseUtils.shortHash("https://nypost.com/new-york-liberty/#Post Sports+#");

        assertNotEquals(a, b);
    }

    @Test
    void shortHashIsTenHexChars() {
        String hash = ParseUtils.shortHash("https://nypost.com/nfl/");

        assertEquals(10, hash.length());
        assertEquals(hash, hash.toLowerCase());
        assertEquals(true, hash.matches("[0-9a-f]{10}"));
    }
}
