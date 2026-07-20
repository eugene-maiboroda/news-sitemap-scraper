package com.article.adapter.export.mapper;

import com.article.adapter.export.dto.AuthorExportDto;
import com.article.adapter.shared.FileNaming;
import com.article.domain.author.Author;
import org.springframework.stereotype.Component;

/**
 * Shared Author -> AuthorExportDto mapping, mirroring ArticleExportMapper. avatarId is never
 * persisted (see Author/AuthorEntity) — it's derived here the same way ArticleExportMapper derives
 * image/video ids, via a FileNaming.*FilePrefix method, so the future avatar-download step can
 * reuse the exact same prefix and always match what was written into the export JSON.
 */
@Component
public class AuthorExportMapper {

    public AuthorExportDto toDto(Author author) {
        String avatarUrl = author.avatarUrl();
        String avatarId = avatarUrl == null ? null : FileNaming.avatarFilePrefix(author.id());

        return new AuthorExportDto(
                author.authorName(),
                author.authorUrl(),
                author.bio(),
                avatarUrl,
                avatarId);
    }
}
