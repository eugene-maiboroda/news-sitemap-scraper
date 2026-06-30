package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.dto.ArticlesExportDto;
import com.article.adapter.shared.ParseUtils;
import com.article.domain.Article;
import com.article.infrastructure.CollectorProps;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonExportAdapter implements ExportPort {

    private final ObjectMapper objectMapper;
    private final CollectorProps collectorProps;

    @Override
    public void export(List<Article> articles) {
        List<ArticleExportDto> dtos = articles.stream()
                .sorted(Comparator.comparing(Article::category, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Article::publishDate))
                .map(this::toDto)
                .toList();

        log.info("[EXPORT] Articles to export: {}", dtos.size());

        try {
            Path exportDir = Path.of(collectorProps.exportDir());
            Files.createDirectories(exportDir);
            objectMapper.writeValue(exportDir.resolve("articles.json").toFile(), new ArticlesExportDto(dtos));
        } catch (IOException e) {
            throw new RuntimeException("Failed to export articles", e);
        }

        log.info("[EXPORT] Done");
    }

    private ArticleExportDto toDto(Article a) {
        return ArticleExportDto.builder()
                .id(a.id())
                .category(a.category())
                .articleUrl(a.url())
                .title(a.title())
                .subTitle(a.subTitle())
                .publishedDate(a.publishDate())
                .author(a.author())
                .authorLink(a.authorLink())
                .imageId(ParseUtils.imageFilePrefix(a.category(), a.id()))
                .imageCaption(a.caption())
                .content(a.content())
                .build();
    }
}
