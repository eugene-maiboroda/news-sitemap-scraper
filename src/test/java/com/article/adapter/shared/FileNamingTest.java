package com.article.adapter.shared;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileNamingTest {

    @Test
    void buildsTwoLevelHierarchy() {
        List<String> hierarchy = FileNaming.buildHierarchy("Sports", List.of("NFL"));

        assertEquals(List.of("Sports", "NFL"), hierarchy);
    }

    @Test
    void dedupesMainCategoryEqualToTopic1() {
        List<String> hierarchy = FileNaming.buildHierarchy("Sports", Arrays.asList("Sports", "NFL"));

        assertEquals(List.of("Sports", "NFL"), hierarchy);
    }

    @Test
    void skipsNullOnIntermediatePosition() {
        // Post Sports+ / topic1=null / topic2=Inside the Knicks
        List<String> hierarchy = FileNaming.buildHierarchy("Post Sports+", Arrays.asList(null, "Inside the Knicks"));

        assertEquals(List.of("Post Sports+", "Inside the Knicks"), hierarchy);
    }

    @Test
    void mainCategoryOnlyWhenNoTopicsResolved() {
        List<String> hierarchy = FileNaming.buildHierarchy("Business", Collections.singletonList(null));

        assertEquals(List.of("Business"), hierarchy);
    }

    @Test
    void shortHashIsDeterministicAcrossCalls() {
        String first = FileNaming.shortHash("https://nypost.com/new-york-liberty/");
        String second = FileNaming.shortHash("https://nypost.com/new-york-liberty/");

        assertEquals(first, second);
    }

    @Test
    void shortHashDiffersForDifferentInput() {
        String a = FileNaming.shortHash("https://nypost.com/new-york-liberty/#Sports#WNBA");
        String b = FileNaming.shortHash("https://nypost.com/new-york-liberty/#Post Sports+#");

        assertNotEquals(a, b);
    }

    @Test
    void shortHashIsTenHexChars() {
        String hash = FileNaming.shortHash("https://nypost.com/nfl/");

        assertEquals(10, hash.length());
        assertEquals(hash, hash.toLowerCase());
        assertTrue(hash.matches("[0-9a-f]{10}"));
    }
}
