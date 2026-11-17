package com.article.port.persistence;

import com.article.domain.ArticleImageRef;

import java.util.List;

public interface ImagePersistencePort {

    List<ArticleImageRef> findPending(int limit);

    void markDownloaded(String id, String localFilePath, long fileSize);

    void markFailed(String id);
}
