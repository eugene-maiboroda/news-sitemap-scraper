package com.article.adapter.image.persistence;

import com.article.adapter.image.mapper.ImageMapper;
import com.article.domain.ArticleImageRef;
import com.article.domain.CategoryPendingCount;
import com.article.domain.Image;
import com.article.port.IdGenerationPort;
import com.article.port.persistence.ImagePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImagePersistenceService implements ImagePersistencePort {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final IdGenerationPort idGenerationPort;

    @Override
    public void save(Image image) {
        image.setId(idGenerationPort.generateId());
        imageRepository.save(imageMapper.toEntity(image));
    }

    @Override
    public boolean existsByArticleIdAndImageUrl(String articleId, String imageUrl) {
        return imageRepository.existsByArticleIdAndImageUrl(articleId, imageUrl);
    }

    @Override
    public List<Image> findImagesByArticleIds(List<String> articleIds) {
        return imageRepository.findByArticleIdIn(articleIds).stream()
                .map(imageMapper::toImage)
                .toList();
    }

    @Override
    public List<CategoryPendingCount> findCategoriesOrderedByPendingCount() {
        return imageRepository.findCategoriesOrderedByPendingCount();
    }

    @Override
    public List<ArticleImageRef> findPendingImagesForCategory(String category, int limit) {
        return imageRepository.findPendingImagesForCategory(category, PageRequest.of(0, limit));
    }

    @Override
    public List<ArticleImageRef> findPendingImagesWithoutCategory(int limit) {
        return imageRepository.findPendingImagesWithoutCategory(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markDownloaded(String id, String localFilePath, long fileSize) {
        imageRepository.markDownloaded(id, localFilePath, fileSize);
    }

    @Override
    @Transactional
    public void markFailed(String id) {
        imageRepository.markFailed(id);
    }
}
