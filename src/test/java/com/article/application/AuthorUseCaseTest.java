package com.article.application;

import com.article.domain.author.Author;
import com.article.port.out.AuthorAvatarDownloadPort;
import com.article.port.out.AuthorExportPort;
import com.article.port.out.AuthorParserPort;
import com.article.port.out.persistence.AuthorPersistencePort;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorUseCaseTest {

    private final AuthorPersistencePort persistence = mock(AuthorPersistencePort.class);
    private final AuthorParserPort parser = mock(AuthorParserPort.class);
    private final ParallelExecutor parallelExecutor = mock(ParallelExecutor.class);
    private final AuthorExportPort exportPort = mock(AuthorExportPort.class);
    private final AuthorAvatarDownloadPort avatarDownloadPort = mock(AuthorAvatarDownloadPort.class);
    private final AuthorUseCase useCase = new AuthorUseCase(
            persistence,
            parser,
            parallelExecutor,
            exportPort,
            avatarDownloadPort,
            mock(PipelineRunSettings.class)
    );

    @Test
    void processesAuthorsInThreeSequentialStages() {
        Author candidate = author("author-1", null, null);
        Author parsed = author("author-1", "avatar-url", null);
        Author downloaded = author("author-1", "avatar-url", "avatar-file");
        List<Author> candidates = List.of(candidate);
        List<Author> parsedAuthors = List.of(parsed);
        List<Author> downloadedAuthors = List.of(downloaded);

        when(persistence.findCandidates(0, 100)).thenReturn(candidates);
        when(persistence.findCandidates(1, 100)).thenReturn(List.of());
        when(persistence.findAll()).thenReturn(candidates);
        when(persistence.findAuthorsPendingAvatarDownload()).thenReturn(parsedAuthors);
        when(parallelExecutor.<Author, Author>map(eq(candidates), any())).thenReturn(parsedAuthors);
        when(parallelExecutor.<Author, Author>map(eq(parsedAuthors), any())).thenReturn(downloadedAuthors);

        useCase.processAuthors(100);

        InOrder order = inOrder(persistence, parallelExecutor);
        order.verify(persistence).findCandidates(0, 100);
        order.verify(persistence).insert(candidates);
        order.verify(persistence).findCandidates(1, 100);
        order.verify(persistence).findAll();
        order.verify(parallelExecutor).map(eq(candidates), any());
        order.verify(persistence).update(parsedAuthors);
        order.verify(persistence).findAuthorsPendingAvatarDownload();
        order.verify(parallelExecutor).map(eq(parsedAuthors), any());
        order.verify(persistence).update(downloadedAuthors);
    }

    @Test
    void exportsAuthorsWithoutRunningProcessingStages() {
        List<Author> authors = List.of(author("author-1", null, null));
        when(persistence.findAll()).thenReturn(authors);

        useCase.exportAuthors();

        verify(exportPort).export(authors);
    }

    private Author author(String id, String avatarUrl, String avatarFileId) {
        return new Author(
                id,
                "Author",
                "https://example.com/author",
                "Bio",
                avatarUrl,
                avatarFileId
        );
    }
}
