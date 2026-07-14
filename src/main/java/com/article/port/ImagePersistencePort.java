package com.article.port;

import com.article.domain.ArticleImageRef;
import com.article.domain.CategoryPendingCount;
import com.article.domain.Image;

import java.util.List;

public interface ImagePersistencePort {

    void save(Image image);

    boolean existsByArticleIdAndImageUrl(String articleId, String imageUrl);

    List<Image> findImagesByArticleIds(List<String> articleIds);

    List<CategoryPendingCount> findCategoriesOrderedByPendingCount();

    List<ArticleImageRef> findPendingImagesForCategory(String category, int limit);

    List<ArticleImageRef> findPendingImagesWithoutCategory(int limit);

    void markDownloaded(String id, String localFilePath, long fileSize);

    void markFailed(String id);
}
