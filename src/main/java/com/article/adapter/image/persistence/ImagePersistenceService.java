package com.article.adapter.image.persistence;

import com.article.adapter.image.mapper.ImageMapper;
import com.article.adapter.image.persistence.entity.ImageEntity;
import com.article.domain.media.Image;
import com.article.domain.media.DownloadStatus;
import com.article.port.out.IdGenerationPort;
import com.article.port.out.persistence.ImagePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImagePersistenceService implements ImagePersistencePort {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final IdGenerationPort idGenerationPort;

    @Override
    public void saveAll(List<Image> images) {
        if (images.isEmpty()) {
            return;
        }
        List<String> articleIds = images.stream()
                .map(Image::articleId)
                .distinct()
                .toList();
        Set<ImageKey> existing = imageRepository.findByArticleIdIn(articleIds).stream()
                .map(entity -> new ImageKey(entity.getArticleId(), entity.getImageUrl()))
                .collect(Collectors.toSet());

        List<ImageEntity> newImages = images.stream()
                .filter(image -> existing.add(new ImageKey(image.articleId(), image.imageUrl())))
                .map(image -> imageMapper.toEntity(image, idGenerationPort.generateId()))
                .toList();
        imageRepository.saveAll(newImages);
    }

    @Override
    public List<Image> findImagesByArticleIds(List<String> articleIds) {
        return imageRepository.findByArticleIdIn(articleIds).stream()
                .map(imageMapper::toImage)
                .toList();
    }

    @Override
    public List<Image> findPendingImages(int limit) {
        return imageRepository.findByDownloadStatusOrderById(
                        DownloadStatus.PENDING,
                        PageRequest.of(0, limit)
                ).stream()
                .map(imageMapper::toImage)
                .toList();
    }

    @Override
    @Transactional
    public void markDownloaded(String id, String localFilePath, long fileSize) {
        imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + id))
                .markDownloaded(localFilePath, fileSize);
    }

    @Override
    @Transactional
    public void markFailed(String id) {
        imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + id))
                .markFailed();
    }

    private record ImageKey(String articleId, String imageUrl) {
    }
}
