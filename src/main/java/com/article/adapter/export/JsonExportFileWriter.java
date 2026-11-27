package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.model.JsonExportTarget;
import com.article.application.PipelineRunSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonExportFileWriter {

    private final ObjectMapper objectMapper;
    private final PipelineRunSettings runSettings;

    private final Map<JsonExportTarget, FileState> files = new LinkedHashMap<>();

    public void initialize(List<JsonExportTarget> targets) {
        targets.forEach(target -> files.computeIfAbsent(target, this::createFile));
    }

    public void append(JsonExportTarget target, List<ArticleExportDto> articles) {
        if (articles.isEmpty()) {
            return;
        }
        FileState file = files.computeIfAbsent(target, this::createFile);
        try (BufferedWriter writer = Files.newBufferedWriter(
                file.path(),
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND
        )) {
            for (ArticleExportDto article : articles) {
                if (file.hasArticles()) {
                    writer.write(',');
                }
                writer.write(objectMapper.writeValueAsString(article));
                file.articleWritten();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append export file: " + file.path(), e);
        }
    }

    public void complete() {
        RuntimeException failure = null;
        try {
            for (Map.Entry<JsonExportTarget, FileState> entry : files.entrySet()) {
                try {
                    Files.writeString(
                            entry.getValue().path(),
                            entry.getKey().isTopic() ? "]}}" : "]}",
                            StandardCharsets.UTF_8,
                            StandardOpenOption.APPEND
                    );
                } catch (IOException | RuntimeException e) {
                    if (failure == null) {
                        failure = new IllegalStateException(
                                "Failed to complete export file: " + entry.getValue().path(),
                                e
                        );
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
        } finally {
            files.clear();
        }
        if (failure != null) {
            throw failure;
        }
    }

    private FileState createFile(JsonExportTarget target) {
        Path path = Path.of(runSettings.current().storage().exportDir()).resolve(target.relativePath());
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(
                    path,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                if (target.isTopic()) {
                    writer.write('{');
                    writer.write(objectMapper.writeValueAsString(target.topicUrl()));
                    writer.write(":{\"hierarchy\":");
                    writer.write(objectMapper.writeValueAsString(target.hierarchy()));
                    writer.write(",\"articles\":[");
                } else {
                    writer.write("{\"articles\":[");
                }
            }
            return new FileState(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create export file: " + path, e);
        }
    }

    private static final class FileState {
        private final Path path;
        private boolean hasArticles;

        private FileState(Path path) {
            this.path = path;
        }

        private Path path() {
            return path;
        }

        private boolean hasArticles() {
            return hasArticles;
        }

        private void articleWritten() {
            hasArticles = true;
        }
    }
}
