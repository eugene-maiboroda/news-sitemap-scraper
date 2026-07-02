package com.article.adapter.image.mapper;

import com.article.adapter.image.persistence.entity.ImageEntity;
import com.article.domain.Image;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(config = MapStructConfig.class)
public interface ImageMapper {

    default String generateId() {
        return UUID.randomUUID().toString();
    }

    @Mapping(target = "id", expression = "java(generateId())")
    @Mapping(target = "createdAt", ignore = true)
    ImageEntity toEntity(Image image);
}
