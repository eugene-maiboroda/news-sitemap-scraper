package com.article.adapter.export;

import com.article.adapter.export.mapper.ArticleExportMapper;
import com.article.application.PipelineRunSettings;
import com.article.bootstrap.properties.BrowserProps;
import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.ExportMode;
import com.article.bootstrap.properties.ExportProps;
import com.article.bootstrap.properties.StepsProps;
import com.article.bootstrap.properties.VideoProps;
import com.article.bootstrap.properties.StorageProps;
import com.article.domain.article.Article;
import com.article.domain.article.TaxonomyLink;
import com.article.domain.article.ArticleMetadata;
import com.article.domain.article.ArticleWithMetadata;
import com.article.application.dto.ArticleExportBatch;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonExportAdapterTest {

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;
    private JsonExportAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ArticleExportMapper mapper = new ArticleExportMapper();
        ArticleExportAssembler assembler = new ArticleExportAssembler(mapper);
        PipelineRunSettings runSettings = new PipelineRunSettings(
                new CollectorProps(1, 0, List.of(), null),
                new StorageProps(tempDir.toString(), null, null, null),
                new ExportProps(ExportMode.TOPICS, null),
                new BrowserProps(false, true, 30000),
                new VideoProps(300, "yt-dlp"),
                new StepsProps(null, null, null, null, null, null, null, null)
        );
        JsonExportFileWriter writer = new JsonExportFileWriter(
                objectMapper,
                runSettings
        );
        adapter = new JsonExportAdapter(assembler, runSettings, writer);
    }

    @AfterEach
    void tearDown() {
        adapter.complete();
    }

    @Test
    void appendsMultipleBatchesToEveryResolvedTopic() throws IOException {
        adapter.export(batch(article("a1", "2026-01-01")));
        adapter.export(batch(article("a2", "2026-01-02")));
        adapter.complete();

        List<Path> files;
        try (var paths = Files.walk(tempDir)) {
            files = paths.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertEquals(2, files.size());
        for (Path file : files) {
            JsonNode root = objectMapper.readTree(file.toFile());
            JsonNode topic = root.elements().next();
            assertEquals(2, topic.get("articles").size());
        }
    }

    private ArticleExportBatch batch(Article article) {
        ArticleMetadata metadata = new ArticleMetadata(
                List.of(
                        new TaxonomyLink("Sports", "/sports"),
                        new TaxonomyLink("NFL", "/nfl")
                ),
                List.of(),
                "Sports"
        );
        return new ArticleExportBatch(
                List.of(new ArticleWithMetadata(article, metadata)),
                List.of(),
                List.of()
        );
    }

    private Article article(String id, String date) {
        return Article.builder()
                .id(id)
                .category("Sports")
                .url("https://example.com/" + id)
                .publishDate(LocalDate.parse(date))
                .title("Title")
                .content("Content")
                .build();
    }
}
