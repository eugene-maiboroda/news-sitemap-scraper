package com.article.adapter.image.mapper;

import com.article.adapter.image.persistence.entity.ImageEntity;
import com.article.domain.media.Image;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = MapStructConfig.class)
public interface ImageMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", source = "id")
    @BeanMapping(ignoreUnmappedSourceProperties = "id")
    ImageEntity toEntity(Image image, String id);

    @BeanMapping(unmappedSourcePolicy = ReportingPolicy.IGNORE)
    Image toImage(ImageEntity entity);
}
