package com.article.application;

import com.article.adapter.author.AuthorParser;
import com.article.adapter.shared.ParseUtils;
import com.article.domain.Author;
import com.article.domain.AuthorParseResult;
import com.article.domain.DownloadResult;
import com.article.domain.status.DownloadStatus;
import com.article.port.ArticlePersistencePort;
import com.article.port.AuthorAvatarDownloadPort;
import com.article.port.AuthorExportPort;
import com.article.port.AuthorPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorUseCase {

    private final ArticlePersistencePort articlePersistencePort;
    private final AuthorPersistencePort authorPersistencePort;
    private final AuthorParser authorParser;
    private final ParallelExecutor parallelExecutor;
    private final AuthorExportPort authorExportPort;
    private final AuthorAvatarDownloadPort authorAvatarDownloadPort;

    public void extractAuthorsFromArticle() {
        List<Author> authors = articlePersistencePort.findDistinctAuthors();
        authorPersistencePort.insert(authors);
    }

    public void parseAuthors() {
        List<Author> authors = authorPersistencePort.findAll();

        List<Author> updatedAuthors = parallelExecutor.map(authors, this::enrichAuthor);

        authorPersistencePort.update(updatedAuthors);
    }

    public void downloadAvatars() {
        List<Author> pending = authorPersistencePort.findAuthorsPendingAvatarDownload();

        List<Author> downloaded = parallelExecutor.map(pending, this::downloadAvatar).stream()
                .filter(author -> author.avatarFileId() != null)
                .toList();

        authorPersistencePort.update(downloaded);
    }

    public void exportAuthors() {
        List<Author> authors = authorPersistencePort.findAll();
        authorExportPort.export(authors);
    }

    private Author enrichAuthor(Author author) {
        return switch (authorParser.parse(author)) {
            case AuthorParseResult.Success success -> success.author();
            case AuthorParseResult.Failure failure -> {
                log.warn("Failed to parse author {}: {}", author.authorUrl(), failure.error().reason());
                yield author;
            }
        };
    }

    private Author downloadAvatar(Author author) {
        DownloadResult result = authorAvatarDownloadPort.download(author);
        if (result.status() != DownloadStatus.DOWNLOADED) {
            log.warn("Failed to download avatar for author {}: {}", author.authorUrl(), author.avatarUrl());
            return author;
        }

        return new Author(
                author.id(),
                author.authorName(),
                author.authorUrl(),
                author.bio(),
                author.avatarUrl(),
                ParseUtils.avatarFilePrefix(author.id()));
    }
}
