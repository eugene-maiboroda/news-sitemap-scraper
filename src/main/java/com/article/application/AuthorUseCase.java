package com.article.application;

import com.article.domain.Author;
import com.article.domain.AuthorParseResult;
import com.article.port.AuthorExportPort;
import com.article.port.AuthorParserPort;
import com.article.port.persistence.ArticlePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorUseCase {

    private final ArticlePersistencePort articles;
    private final AuthorParserPort parser;
    private final AuthorExportPort exporter;
    private final ParallelExecutor parallelExecutor;

    public void extractAuthors() {
        List<Author> authors = articles.findDistinctAuthors();
        List<Author> profiles = parallelExecutor.map(authors, this::readProfile);
        if (profiles.size() != authors.size()) {
            throw new IllegalStateException("Author profile collection did not finish");
        }

        exporter.export(profiles);
        log.info("Exported {} authors", profiles.size());
    }

    private Author readProfile(Author author) {
        return switch (parser.parse(author)) {
            case AuthorParseResult.Success success -> success.author();
            case AuthorParseResult.Failure failure -> {
                log.warn("Failed to parse author {}: {}", author.authorUrl(), failure.error().reason());
                yield author;
            }
        };
    }
}
