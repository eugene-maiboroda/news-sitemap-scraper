package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.model.JsonExportTarget;
import com.article.bootstrap.properties.StorageProps;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonExportFileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void appendsCategoryBatchesAndCompletesValidJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonExportFileWriter writer = new JsonExportFileWriter(
                objectMapper,
                new StorageProps(tempDir.toString(), null, null, null)
        );
        JsonExportTarget target = JsonExportTarget.category("sports.json");

        writer.append(target, List.of(ArticleExportDto.builder().id("a1").build()));
        writer.append(target, List.of(ArticleExportDto.builder().id("a2").build()));
        writer.complete();

        JsonNode root = objectMapper.readTree(tempDir.resolve("sports.json").toFile());
        assertEquals(2, root.get("articles").size());
    }
}
