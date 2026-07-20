package com.article.adapter.export;

import com.article.bootstrap.properties.StorageProps;
import com.article.domain.Author;
import com.article.port.AuthorExportPort;
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
    private final StorageProps storageProps;
    private final AuthorExportMapper authorExportMapper;

    @Override
    public void export(List<Author> authors) {
        try {
            Path exportDir = Path.of(storageProps.exportDir());
            Files.createDirectories(exportDir);

            JsonGenerator generator = objectMapper.getFactory().createGenerator(
                    exportDir.resolve(FILE_NAME).toFile(), JsonEncoding.UTF8);
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeArrayFieldStart("authors");

            for (Author author : authors) {
                objectMapper.writeValue(generator, authorExportMapper.toDto(author));
            }

            generator.writeEndArray();
            generator.writeEndObject();
            generator.close();

            log.info("[EXPORT][authors] {} -> {} authors", FILE_NAME, authors.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export authors to " + FILE_NAME, e);
        }
    }
}
