package com.article.port;

import com.article.domain.Image;

import java.util.List;

public interface ImagePersistencePort {

    void save(Image image);

    boolean existsByArticleIdAndImageUrl(String articleId, String imageUrl);

    List<Image> findAllImages();
}
