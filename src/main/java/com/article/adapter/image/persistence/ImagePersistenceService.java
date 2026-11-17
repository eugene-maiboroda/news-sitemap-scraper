package com.article.adapter.image.persistence;

import com.article.adapter.image.mapper.ImageMapper;
import com.article.domain.ArticleImageRef;
import com.article.domain.CategoryPendingCount;
import com.article.domain.Image;
import com.article.port.IdGenerationPort;
import com.article.port.persistence.ImagePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImagePersistenceService implements ImagePersistencePort {

    private final ObjectProvider<ImageRepository> imageRepositoryProvider;
    private final ImageMapper imageMapper;
    private final IdGenerationPort idGenerationPort;

    @Override
    public void save(Image image) {
        image.setId(idGenerationPort.generateId());
        imageRepositoryProvider.getObject().save(imageMapper.toEntity(image));
    }

    @Override
    public boolean existsByArticleIdAndImageUrl(String articleId, String imageUrl) {
        return imageRepositoryProvider.getObject().existsByArticleIdAndImageUrl(articleId, imageUrl);
    }

    @Override
    public List<Image> findImagesByArticleIds(List<String> articleIds) {
        return imageRepositoryProvider.getObject().findByArticleIdIn(articleIds).stream()
                .map(imageMapper::toImage)
                .toList();
    }

    @Override
    public List<CategoryPendingCount> findCategoriesOrderedByPendingCount() {
        return imageRepositoryProvider.getObject().findCategoriesOrderedByPendingCount();
    }

    @Override
    public List<ArticleImageRef> findPendingImagesForCategory(String category, int limit) {
        return imageRepositoryProvider.getObject().findPendingImagesForCategory(category, PageRequest.of(0, limit));
    }

    @Override
    public List<ArticleImageRef> findPendingImagesWithoutCategory(int limit) {
        return imageRepositoryProvider.getObject().findPendingImagesWithoutCategory(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markDownloaded(String id, String localFilePath, long fileSize) {
        imageRepositoryProvider.getObject().markDownloaded(id, localFilePath, fileSize);
    }

    @Override
    @Transactional
    public void markFailed(String id) {
        imageRepositoryProvider.getObject().markFailed(id);
    }
}
