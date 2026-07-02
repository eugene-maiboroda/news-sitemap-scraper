package com.article.adapter.image.persistence;

import com.article.adapter.image.persistence.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface ImageRepository extends JpaRepository<ImageEntity, String> {

    boolean existsByArticleIdAndImageUrl(String articleId, String imageUrl);
}
