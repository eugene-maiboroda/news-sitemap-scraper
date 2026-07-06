package com.article.adapter.image.mapper;

import com.article.adapter.image.persistence.entity.ImageEntity;
import com.article.domain.Image;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface ImageMapper {

    @Mapping(target = "createdAt", ignore = true)
    ImageEntity toEntity(Image image);
}
