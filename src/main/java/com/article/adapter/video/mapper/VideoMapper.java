package com.article.adapter.video.mapper;

import com.article.adapter.video.persistence.entity.VideoEntity;
import com.article.domain.Video;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(config = MapStructConfig.class)
public interface VideoMapper {

    default String generateId() {
        return UUID.randomUUID().toString();
    }

    @Mapping(target = "id", expression = "java(generateId())")
    @Mapping(target = "createdAt", ignore = true)
    VideoEntity toEntity(Video video);
}
