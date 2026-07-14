package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.dto.ImageExportDto;
import com.article.adapter.export.dto.LinkDto;
import com.article.adapter.export.dto.VideoExportDto;
import com.article.adapter.shared.ParseUtils;
import com.article.domain.Article;
import com.article.domain.Image;
import com.article.domain.Video;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Shared Article+images+videos+topics+tags -> ArticleExportDto mapping, used by both export
 * adapters. Deliberately holds only this pure mapping — streaming/JsonGenerator lifecycle stays
 * independent per adapter (see JsonExportAdapter/TopicJsonExportAdapter), since that logic may
 * diverge between category and topic export in ways this shared mapping should not be coupled to.
 */
@Component
public class ArticleExportMapper {

    public ArticleExportDto toDto(Article a, List<Image> images, List<Video> videos,
                                   List<LinkDto> topics, List<LinkDto> tags) {

        List<ImageExportDto> imageDtos = images.stream()
                .sorted(Comparator.comparing(Image::getPosition))
                .map(image -> new ImageExportDto(
                        ParseUtils.imageFilePrefix(
                                a.category(),
                                a.id(),
                                image.getPosition()),
                        image.getCaption(),
                        image.getImageUrl()))
                .toList();

        List<VideoExportDto> videoDtos = videos.stream()
                .map(video -> new VideoExportDto(
                        ParseUtils.videoFilePrefix(
                                a.category(),
                                a.id(),
                                video.getPlatform().name(),
                                ParseUtils.shortId(video.getId())),
                        video.getM3u8Url()))
                .toList();

        return ArticleExportDto.builder()
                .id(a.id())
                .category(a.category())
                .articleUrl(a.url())
                .title(a.title())
                .subTitle(a.subTitle())
                .publishedDate(a.publishDate())
                .author(a.author())
                .authorLink(a.authorLink())
                .images(imageDtos)
                .videos(videoDtos)
                .topics(topics)
                .tags(tags)
                .content(a.content())
                .build();
    }
}
