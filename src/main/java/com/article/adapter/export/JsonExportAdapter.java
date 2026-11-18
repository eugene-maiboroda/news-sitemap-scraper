package com.article.adapter.export;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@RequiredArgsConstructor
public class JsonExportAdapter {

    private final ObjectMapper objectMapper;

    public void export(Path output, String arrayName, Iterable<?> entries) throws IOException {
        export(output, arrayName, writer -> {
            for (Object entry : entries) {
                writer.write(entry);
            }
        });
    }

    void export(Path output, String arrayName, Entries entries) throws IOException {
        Path target = output.toAbsolutePath();
        Path directory = target.getParent();
        Files.createDirectories(directory);

        Path temporary = Files.createTempFile(directory, "export-", ".json");
        try {
            writeEntries(temporary, arrayName, entries);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void writeEntries(Path output, String arrayName, Entries entries) throws IOException {
        try (JsonGenerator generator = objectMapper.getFactory().createGenerator(output.toFile(), JsonEncoding.UTF8)) {
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeArrayFieldStart(arrayName);
            entries.writeTo(entry -> objectMapper.writeValue(generator, entry));
            generator.writeEndArray();
            generator.writeEndObject();
        }
    }
    @FunctionalInterface
    interface Entries {

        void writeTo(EntryWriter writer) throws IOException;
    }

    @FunctionalInterface
    interface EntryWriter {

        void write(Object entry) throws IOException;
    }
}
