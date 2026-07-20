package com.article.adapter.video.mapper;

import com.article.adapter.video.persistence.entity.VideoEntity;
import com.article.domain.media.Video;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = MapStructConfig.class)
public interface VideoMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", source = "id")
    @BeanMapping(ignoreUnmappedSourceProperties = "id")
    VideoEntity toEntity(Video video, String id);

    @BeanMapping(unmappedSourcePolicy = ReportingPolicy.IGNORE)
    Video toVideo(VideoEntity entity);
}
