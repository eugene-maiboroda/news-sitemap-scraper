package com.article.adapter.export;

import com.article.adapter.export.mapper.AuthorExportMapper;
import com.article.application.PipelineRunSettings;
import com.article.domain.author.Author;
import com.article.port.out.AuthorExportPort;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports the full authors table into a single AllAuthors.json ({"authors": [...]}), same
 * writeStartObject/writeArrayFieldStart streaming style as JsonExportAdapter. Unlike the article
 * export adapters, callers always pass the entire author list in one call (AuthorUseCase loads it
 * all via findAll()), so there's no cross-call generator lifecycle to manage here — open, write,
 * close within a single export() invocation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorJsonExportAdapter implements AuthorExportPort {

    private static final String FILE_NAME = "AllAuthors.json";

    private final ObjectMapper objectMapper;
    private final PipelineRunSettings runSettings;
    private final AuthorExportMapper authorExportMapper;

    @Override
    public void export(List<Author> authors) {
        try {
            Path exportDir = Path.of(runSettings.current().storage().exportDir());
            Files.createDirectories(exportDir);
            int written = 0;

            try (JsonGenerator generator = objectMapper.getFactory().createGenerator(
                    exportDir.resolve(FILE_NAME).toFile(), JsonEncoding.UTF8
            )) {
                generator.useDefaultPrettyPrinter();
                generator.writeStartObject();
                generator.writeArrayFieldStart("authors");

                for (Author author : authors) {
                    if (runSettings.isStopRequested()) {
                        break;
                    }
                    objectMapper.writeValue(generator, authorExportMapper.toDto(author));
                    written++;
                }

                generator.writeEndArray();
                generator.writeEndObject();
            }

            log.info("[EXPORT][authors] {} -> {} authors", FILE_NAME, written);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export authors to " + FILE_NAME, e);
        }
    }
}
