package com.article.domain.author;

import lombok.Builder;

@Builder
public record Author(
        String id,
        String authorName,
        String authorUrl,
        String bio,
        String avatarUrl,
        String avatarFileId
) {
}
