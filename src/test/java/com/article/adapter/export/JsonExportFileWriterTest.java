package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.model.JsonExportTarget;
import com.article.application.PipelineRunSettings;
import com.article.bootstrap.properties.BrowserProps;
import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.ExportMode;
import com.article.bootstrap.properties.ExportProps;
import com.article.bootstrap.properties.StepsProps;
import com.article.bootstrap.properties.VideoProps;
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
        PipelineRunSettings runSettings = new PipelineRunSettings(
                new CollectorProps(1, 0, List.of(), null),
                new StorageProps(tempDir.toString(), null, null, null),
                new ExportProps(ExportMode.CATEGORY, null),
                new BrowserProps(false, true, 30000),
                new VideoProps(300, "yt-dlp"),
                new StepsProps(null, null, null, null, null, null, null, null)
        );
        JsonExportFileWriter writer = new JsonExportFileWriter(
                objectMapper,
                runSettings
        );
        JsonExportTarget target = JsonExportTarget.category("sports.json");

        writer.append(target, List.of(ArticleExportDto.builder().id("a1").build()));
        writer.append(target, List.of(ArticleExportDto.builder().id("a2").build()));
        writer.complete();

        JsonNode root = objectMapper.readTree(tempDir.resolve("sports.json").toFile());
        assertEquals(2, root.get("articles").size());
    }
}
