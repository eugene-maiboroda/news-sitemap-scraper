package com.article.adapter.export;

import com.article.adapter.export.dto.AuthorExportDto;
import com.article.bootstrap.properties.StorageProps;
import com.article.domain.Author;
import com.article.port.AuthorExportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthorJsonExportAdapter implements AuthorExportPort {

    private final JsonExportAdapter writer;
    private final StorageProps storage;

    @Override
    public void export(List<Author> authors) {
        Path output = Path.of(storage.exportDir()).resolve("AllAuthors.json");
        Iterable<AuthorExportDto> profiles = () -> authors.stream()
                .map(author -> new AuthorExportDto(
                        author.authorName(),
                        author.authorUrl(),
                        author.bio(),
                        author.avatarUrl(),
                        author.avatarFileId()
                ))
                .iterator();
        try {
            writer.export(output, "authors", profiles);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write author export " + output, e);
        }
    }
}
