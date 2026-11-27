package com.article.application;

import com.article.domain.author.Author;
import com.article.application.dto.AuthorParseResult;
import com.article.application.dto.DownloadResult;
import com.article.domain.media.DownloadStatus;
import com.article.port.out.AuthorAvatarDownloadPort;
import com.article.port.out.AuthorExportPort;
import com.article.port.out.AuthorParserPort;
import com.article.port.out.persistence.AuthorPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorUseCase {

    private final AuthorPersistencePort authorPersistencePort;
    private final AuthorParserPort authorParser;
    private final ParallelExecutor parallelExecutor;
    private final AuthorExportPort authorExportPort;
    private final AuthorAvatarDownloadPort authorAvatarDownloadPort;
    private final PipelineRunSettings runSettings;

    public void processAuthors(int batchSize) {
        log.info("Step: PROCESS AUTHORS");
        int extracted = extractAuthorsFromArticle(batchSize);
        int parsed = runSettings.isStopRequested() ? 0 : parseAuthors();
        int avatars = runSettings.isStopRequested() ? 0 : downloadAvatars();
        log.info(
                "Author processing finished ({} candidates processed, {} parsed, {} avatars downloaded)",
                extracted,
                parsed,
                avatars
        );
    }

    private int extractAuthorsFromArticle(int batchSize) {
        int page = 0;
        int extracted = 0;
        List<Author> authors;
        while (!runSettings.isStopRequested()
                && !(authors = authorPersistencePort.findCandidates(page, batchSize)).isEmpty()) {
            authorPersistencePort.insert(authors);
            extracted += authors.size();
            page++;
        }
        return extracted;
    }

    private int parseAuthors() {
        List<Author> authors = authorPersistencePort.findAll();
        List<Author> updatedAuthors = parallelExecutor.map(authors, this::enrichAuthor);
        authorPersistencePort.update(updatedAuthors);
        return updatedAuthors.size();
    }

    private int downloadAvatars() {
        List<Author> pending = authorPersistencePort.findAuthorsPendingAvatarDownload();

        List<Author> downloaded = parallelExecutor.map(pending, this::downloadAvatar).stream()
                .filter(author -> author.avatarFileId() != null)
                .toList();

        authorPersistencePort.update(downloaded);
        return downloaded.size();
    }

    public void exportAuthors() {
        log.info("Step: EXPORT AUTHORS");
        if (runSettings.isStopRequested()) {
            return;
        }
        List<Author> authors = authorPersistencePort.findAll();
        authorExportPort.export(authors);
        if (runSettings.isStopRequested()) {
            log.info("Author export stopped");
        } else {
            log.info("Author export finished ({} authors)", authors.size());
        }
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
                result.storedFileId());
    }
}
