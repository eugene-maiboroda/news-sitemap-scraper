package com.article.port.persistence;

import com.article.domain.ArticleVideoRef;

import java.util.List;

public interface VideoPersistencePort {

    List<ArticleVideoRef> findPending(int limit);

    void markDownloaded(String id, String localFilePath, long fileSize);

    void markFailed(String id);
}
