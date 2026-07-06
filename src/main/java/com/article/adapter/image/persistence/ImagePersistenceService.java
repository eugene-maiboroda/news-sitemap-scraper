package com.article.adapter.image.persistence;

import com.article.adapter.image.mapper.ImageMapper;
import com.article.domain.Image;
import com.article.port.IdGenerationPort;
import com.article.port.ImagePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public List<Image> findAllImages() {
        return imageRepository.findAll().stream()
                .map(imageMapper::toImage)
                .toList();
    }
}
