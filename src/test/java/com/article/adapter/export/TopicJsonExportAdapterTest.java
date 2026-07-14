package com.article.adapter.export;

import com.article.application.dto.ExportData;
import com.article.application.dto.TopicExportContext;
import com.article.bootstrap.CollectorProps;
import com.article.domain.Article;
import com.article.domain.TopicTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicJsonExportAdapterTest {

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;
    private ArticleExportMapper articleExportMapper;
    private TopicJsonExportAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        articleExportMapper = new ArticleExportMapper();
        CollectorProps props = new CollectorProps(null, null, 0, 0, 0, 0,
                tempDir.toString(), null, null, 0, 0, null, null);
        adapter = new TopicJsonExportAdapter(objectMapper, props, articleExportMapper);
    }

    @AfterEach
    void tearDown() {
        adapter.shutdown();
    }

    @Test
    void twoBatchesOfSameTargetProduceOneFileWithBothArticles() throws IOException {
        TopicTarget target = new TopicTarget("https://nypost.com/nfl/", 1, "Sports", Arrays.asList("NFL"));

        adapter.export(exportDataFor(target, article("a1", "2026-01-01")));
        adapter.export(exportDataFor(target, article("a2", "2026-01-02")));
        adapter.shutdown(); // file only becomes valid (closed) JSON once the generator is closed

        List<Path> files = listJsonFiles();
        assertEquals(1, files.size());

        JsonNode root = objectMapper.readTree(files.get(0).toFile());
        JsonNode topicNode = root.get(target.topicUrl());
        assertEquals(List.of("Sports", "NFL"), toList(topicNode.get("hierarchy")));
        assertEquals(2, topicNode.get("articles").size());
    }

    @Test
    void differentTargetSharingSameUrlOpensASeparateFile() throws IOException {
        TopicTarget wnba = new TopicTarget("https://nypost.com/new-york-liberty/", 2, "Sports", Arrays.asList("WNBA", "New York Liberty"));
        TopicTarget postSportsPlus = new TopicTarget("https://nypost.com/new-york-liberty/", 2, "Post Sports+", Arrays.asList(null, "New York Liberty"));

        adapter.export(exportDataFor(wnba, article("a1", "2026-01-01")));
        adapter.export(exportDataFor(postSportsPlus, article("a2", "2026-01-02")));
        adapter.shutdown(); // close the still-open second file before reading it back

        List<Path> files = listJsonFiles();
        assertEquals(2, files.size(), "two different true keys sharing the same URL must produce two separate files");

        String name1 = files.get(0).getFileName().toString();
        String name2 = files.get(1).getFileName().toString();
        assertNotEquals(name1, name2);

        for (Path file : files) {
            JsonNode root = objectMapper.readTree(file.toFile());
            assertTrue(root.has("https://nypost.com/new-york-liberty/"));
            assertEquals(1, root.get("https://nypost.com/new-york-liberty/").get("articles").size());
        }
    }

    @Test
    void fileIsWrittenUnderSlugSubdirectoryWithFullSlugStillInFilename() throws IOException {
        TopicTarget target = new TopicTarget("https://nypost.com/alexa-hamptons/", 2, "Lifestyle",
                Arrays.asList("Lifestyle", "Alexa Hamptons"));

        adapter.export(exportDataFor(target, article("a1", "2026-01-01")));
        adapter.shutdown();

        List<Path> files = listJsonFiles();
        assertEquals(1, files.size());

        Path file = files.get(0);
        String expectedSlug = "alexa_hamptons";
        assertEquals(expectedSlug, file.getParent().getFileName().toString(),
                "file must live directly under a subdirectory named after the readable slug");
        assertEquals(tempDir, file.getParent().getParent(),
                "the slug subdirectory must live directly under the configured export dir");
        assertTrue(file.getFileName().toString().startsWith(expectedSlug + "_"),
                "filename must still start with the full slug, not just the hash");
    }

    @Test
    void shutdownClosesInFlightFileIntoValidJson() throws IOException {
        TopicTarget target = new TopicTarget("https://nypost.com/nfl/", 1, "Sports", Arrays.asList("NFL"));
        adapter.export(exportDataFor(target, article("a1", "2026-01-01")));

        adapter.shutdown();

        List<Path> files = listJsonFiles();
        assertEquals(1, files.size());
        objectMapper.readTree(files.get(0).toFile()); // throws if invalid JSON
    }

    private ExportData exportDataFor(TopicTarget target, Article article) {
        return new ExportData(null, List.of(article), List.of(), List.of(), List.of(),
                new TopicExportContext(target, Map.of()));
    }

    private Article article(String id, String publishDate) {
        return Article.builder()
                .id(id)
                .category("sports")
                .url("https://nypost.com/2026/01/01/" + id + "/")
                .publishDate(LocalDate.parse(publishDate))
                .title("Title " + id)
                .subTitle(null)
                .author("Author")
                .authorLink(null)
                .content("Content")
                .build();
    }

    private List<Path> listJsonFiles() throws IOException {
        try (var stream = Files.walk(tempDir)) {
            return stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private List<String> toList(JsonNode arrayNode) {
        return arrayNode.valueStream().map(JsonNode::asText).toList();
    }
}
