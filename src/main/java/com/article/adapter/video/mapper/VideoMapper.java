package com.article.adapter.video.mapper;

import com.article.adapter.video.persistence.entity.VideoEntity;
import com.article.domain.Video;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = MapStructConfig.class, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface VideoMapper {

    @Mapping(target = "createdAt", ignore = true)
    VideoEntity toEntity(Video video);

    Video toVideo(VideoEntity entity);
}
