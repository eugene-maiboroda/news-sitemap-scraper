package com.article.port;

import com.article.domain.Image;

public interface ImageRepositoryPort {

    void save(Image image);

    boolean existsByArticleIdAndImageUrl(String articleId, String imageUrl);
}
