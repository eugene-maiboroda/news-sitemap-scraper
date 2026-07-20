package com.article.port.out.persistence;

import com.article.domain.media.Image;

import java.util.List;

public interface ImagePersistencePort {

    void saveAll(List<Image> images);

    List<Image> findImagesByArticleIds(List<String> articleIds);

    List<Image> findPendingImages(int limit);

    void markDownloaded(String id, String localFilePath, long fileSize);

    void markFailed(String id);
}
