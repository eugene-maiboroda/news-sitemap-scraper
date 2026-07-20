package com.article.adapter.export.mapper;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.dto.ImageExportDto;
import com.article.adapter.export.dto.LinkDto;
import com.article.adapter.export.dto.VideoExportDto;
import com.article.adapter.shared.FileNaming;
import com.article.domain.article.Article;
import com.article.domain.article.TaxonomyLink;
import com.article.domain.media.Image;
import com.article.domain.media.Video;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ArticleExportMapper {

    public ArticleExportDto toDto(Article a, List<Image> images, List<Video> videos,
                                   List<TaxonomyLink> topics, List<TaxonomyLink> tags) {

        List<ImageExportDto> imageDtos = images.stream()
                .sorted(Comparator.comparing(Image::position))
                .map(image -> new ImageExportDto(
                        FileNaming.imageFilePrefix(
                                a.category(),
                                a.id(),
                                image.position()),
                        image.caption(),
                        image.imageUrl()))
                .toList();

        List<VideoExportDto> videoDtos = videos.stream()
                .map(video -> new VideoExportDto(
                        FileNaming.videoFilePrefix(
                                a.category(),
                                a.id(),
                                video.platform().name(),
                                FileNaming.shortId(video.id())),
                        videoUrl(video)))
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
                .topics(toLinkDtos(topics))
                .tags(toLinkDtos(tags))
                .content(a.content())
                .build();
    }

    private List<LinkDto> toLinkDtos(List<TaxonomyLink> links) {
        return links.stream()
                .map(link -> new LinkDto(link.name(), link.url()))
                .toList();
    }

    private String videoUrl(Video video) {
        if (video.m3u8Url() != null) {
            return video.m3u8Url();
        }
        if (video.youtubeVideoId() != null) {
            return "https://www.youtube.com/watch?v=" + video.youtubeVideoId();
        }
        return null;
    }
}
