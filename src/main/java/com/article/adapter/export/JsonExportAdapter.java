package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.dto.ArticlesExportDto;
import com.article.adapter.shared.ParseUtils;
import com.article.application.dto.ExportData;
import com.article.domain.Article;
import com.article.bootstrap.CollectorProps;
import com.article.domain.Image;
import com.article.domain.Video;
import com.article.port.ExportPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonExportAdapter implements ExportPort {

    private final ObjectMapper objectMapper;
    private final CollectorProps collectorProps;

    @Override
    public void export(ExportData data) {
        Map<String, List<Image>> imagesByArticleId = data.images().stream()
                .collect(Collectors.groupingBy(Image::getArticleId));

        Map<String, List<Video>> videosByArticleId = data.videos().stream()
                .collect(Collectors.groupingBy(Video::getArticleId));

        Map<String, List<Article>> articlesByCategory = data.articles().stream()
                .collect(Collectors.groupingBy(article -> categoryFileName(article.category())));

        log.info("[EXPORT] Articles to export: {}", data.articles().size());

        String logCategory = "";
        try {
            Path exportDir = Path.of(collectorProps.exportDir());
            Files.createDirectories(exportDir);
            for (Map.Entry<String, List<Article>> entry : articlesByCategory.entrySet()) {
                String category = entry.getKey();
                logCategory = category;

                List<ArticleExportDto> articles = entry.getValue().stream()
                        .sorted(Comparator.comparing(Article::publishDate))
                        .map(article -> toDto(
                                article,
                                imagesByArticleId.getOrDefault(article.id(), List.of()),
                                videosByArticleId.getOrDefault(article.id(), List.of())))
                        .toList();

                objectMapper.writeValue(exportDir.resolve(category + ".json").toFile(),
                        new ArticlesExportDto(articles));

                log.info("[EXPORT] {}.json -> {} articles", category, articles.size());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to export category: " + logCategory, e);
        }

        log.info("[EXPORT] Done");
    }

    private ArticleExportDto toDto(Article a, List<Image> images, List<Video> videos) {

        List<String> imageIds = images.stream()
                .sorted(Comparator.comparing(Image::getPosition))
                .map(image -> ParseUtils.imageFilePrefix(
                        a.category(),
                        a.id(),
                        image.getPosition()))
                .toList();

        List<String> videoIds = videos.stream()
                .map(video -> ParseUtils.videoFilePrefix(
                        a.category(),
                        a.id(),
                        video.getPlatform().toString(),
                        shortId(video.getId())))
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
                .images(imageIds)
                .videos(videoIds)
                .content(a.content())
                .build();
    }
    private String categoryFileName(String category) {
        if (category == null || category.isBlank()) {
            return "uncategorized";
        }
        return ParseUtils.categorySlug(category);
    }

    private String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "unknown";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }
}
