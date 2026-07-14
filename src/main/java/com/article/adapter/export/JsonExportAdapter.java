package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.shared.ParseUtils;
import com.article.application.dto.ExportData;
import com.article.domain.Article;
import com.article.domain.ArticleLinks;
import com.article.bootstrap.CollectorProps;
import com.article.domain.Image;
import com.article.domain.Video;
import com.article.port.ExportPort;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * export() is now called once per page of a category (see ArticleCollectorService.exportJson()),
 * not once for the whole category — otherwise a large category has to be fully materialized in
 * memory before writing. To still produce exactly one valid JSON file per category, this adapter
 * keeps a single open {@link JsonGenerator} across calls and only closes it when the category
 * changes (or at shutdown, for the last one) — streaming the "articles" array across batches
 * instead of building it all in memory. ExportPort itself has no "this is the last batch" signal,
 * so detecting the category boundary from the sequence of calls is the only option here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "collector.export.mode", havingValue = "category", matchIfMissing = true)
public class JsonExportAdapter implements ExportPort {

    private final ObjectMapper objectMapper;
    private final CollectorProps collectorProps;
    private final ArticleExportMapper articleExportMapper;

    private String openCategory;
    private JsonGenerator generator;

    @Override
    public void export(ExportData data) {
        String category = categoryFileName(data.category());

        if (!category.equals(openCategory)) {
            closeCurrent();
            openNew(category);
        }

        try {
            writeBatch(data);
        } catch (IOException e) {
            // the array may already contain a partially-written element at this point, so
            // appending a closing ]} would just dress up invalid JSON as if it were valid —
            // close the stream as-is and leave the file honestly broken instead.
            log.error("[EXPORT] Failed mid-batch for category {}, file left incomplete", category, e);
            closeQuietly();
            throw new RuntimeException("Failed to export batch for category: " + category, e);
        }
    }

    private void openNew(String category) {
        try {
            Path exportDir = Path.of(collectorProps.exportDir());
            Files.createDirectories(exportDir);
            generator = objectMapper.getFactory().createGenerator(
                    exportDir.resolve(category + ".json").toFile(), JsonEncoding.UTF8);
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeArrayFieldStart("articles");
            openCategory = category;
        } catch (IOException e) {
            throw new RuntimeException("Failed to open export file for category: " + category, e);
        }
    }

    private void writeBatch(ExportData data) throws IOException {
        Map<String, List<Image>> imagesByArticleId = data.images().stream()
                .collect(Collectors.groupingBy(Image::getArticleId));

        Map<String, List<Video>> videosByArticleId = data.videos().stream()
                .collect(Collectors.groupingBy(Video::getArticleId));

        Map<String, ArticleLinks> linksByArticleId = data.articleLinks().stream()
                .collect(Collectors.toMap(ArticleLinks::articleId, Function.identity()));

        List<ArticleExportDto> articles = data.articles().stream()
                .sorted(Comparator.comparing(Article::publishDate))
                .map(article -> {
                    ArticleLinks links = linksByArticleId.get(article.id());
                    return articleExportMapper.toDto(
                            article,
                            imagesByArticleId.getOrDefault(article.id(), List.of()),
                            videosByArticleId.getOrDefault(article.id(), List.of()),
                            links != null ? links.topics() : List.of(),
                            links != null ? links.tags() : List.of());
                })
                .toList();

        for (ArticleExportDto dto : articles) {
            objectMapper.writeValue(generator, dto);
        }

        log.info("[EXPORT] {}.json -> +{} articles", openCategory, articles.size());
    }

    /** Closes the currently open category file properly. Used on a normal category switch and
     *  at shutdown — in both cases the generator is in a clean, structurally valid state. */
    private void closeCurrent() {
        if (generator == null) {
            return;
        }
        String category = openCategory;
        try {
            generator.writeEndArray();
            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close export file for category: " + category, e);
        } finally {
            generator = null;
            openCategory = null;
        }
    }

    /** Best-effort close after a failure already in flight — never throws (so it can't mask the
     *  original exception), and never attempts to write closing brackets onto a broken array. */
    private void closeQuietly() {
        if (generator == null) {
            return;
        }
        try {
            generator.close();
        } catch (IOException e) {
            log.warn("[EXPORT] Failed to close broken generator for category {}", openCategory, e);
        } finally {
            generator = null;
            openCategory = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            closeCurrent();
        } catch (RuntimeException e) {
            log.warn("[EXPORT] Failed to close export file for category {} during shutdown", openCategory, e);
        }
    }

    private String categoryFileName(String category) {
        if (category == null || category.isBlank()) {
            return "uncategorized";
        }
        return ParseUtils.categorySlug(category);
    }

}
