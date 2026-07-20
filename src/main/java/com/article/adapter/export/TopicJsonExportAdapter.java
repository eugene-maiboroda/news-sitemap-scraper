package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.shared.ParseUtils;
import com.article.application.dto.ExportData;
import com.article.application.dto.TopicExportContext;
import com.article.bootstrap.properties.StorageProps;
import com.article.domain.Article;
import com.article.domain.ArticleLinkValue;
import com.article.domain.ArticleLinks;
import com.article.domain.Image;
import com.article.domain.TopicTarget;
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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Streaming lifecycle mirrors JsonExportAdapter (independent copy, not shared — the two export
 * modes may diverge, same reasoning as downloadBatches/downloadVideoBatches not being shared),
 * with one extra nesting level: root object -> "{topicUrl}" object -> hierarchy array + articles
 * array. A batch changes file whenever the {@link TopicTarget} differs from the currently open
 * one (by value, via record equals()) — comparing on the full true key (topicUrl+level+
 * mainCategory+topicNames), not the deduplicated display hierarchy, since two different true keys
 * can produce the same displayed hierarchy (see targetKey()).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "export.mode", havingValue = "topics")
public class TopicJsonExportAdapter implements ExportPort {

    private final ObjectMapper objectMapper;
    private final StorageProps storageProps;
    private final ArticleExportMapper articleExportMapper;

    private TopicTarget openTarget;
    private JsonGenerator generator;

    @Override
    public void export(ExportData data) {
        TopicTarget target = data.topicContext().target();

        if (!target.equals(openTarget)) {
            closeCurrent();
            openNew(target);
        }

        try {
            writeBatch(data);
        } catch (IOException e) {
            // the array may already contain a partially-written element at this point, so
            // appending closing brackets would just dress up invalid JSON as if it were valid —
            // close the stream as-is and leave the file honestly broken instead.
            log.error("[EXPORT][topics] Failed mid-batch for target {}, file left incomplete", target, e);
            closeQuietly();
            throw new RuntimeException("Failed to export batch for topic: " + target.topicUrl(), e);
        }
    }

    private void openNew(TopicTarget target) {
        List<String> hierarchy = ParseUtils.buildHierarchy(target.mainCategory(), target.topicNames());
        String slug = ParseUtils.categorySlug(hierarchy.get(hierarchy.size() - 1));
        String fileName = slug + "_" + ParseUtils.shortHash(targetKey(target)) + ".json";
        try {
            Path topicDirectory = Path.of(storageProps.exportDir()).resolve(slug);
            Files.createDirectories(topicDirectory);
            generator = objectMapper.getFactory().createGenerator(
                    topicDirectory.resolve(fileName).toFile(), JsonEncoding.UTF8);
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();                          // root
            generator.writeObjectFieldStart(target.topicUrl());    // "<topicUrl>": {
            generator.writeArrayFieldStart("hierarchy");
            for (String level : hierarchy) {
                generator.writeString(level);
            }
            generator.writeEndArray();
            generator.writeArrayFieldStart("articles");
            openTarget = target;
            log.info("[EXPORT][topics] opened {} for {}", fileName, target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open export file for topic: " + target.topicUrl(), e);
        }
    }

    // Same field set as the discovery/membership queries (topicUrl+level+mainCategory+
    // topicNames), NOT the deduplicated display hierarchy: two different true keys can
    // theoretically produce the same displayed hierarchy after dedup (e.g. mainCategory=null vs.
    // mainCategory equal to topic1Name both yield the same visible first entry), so hashing the
    // hierarchy instead could collide two genuinely different targets onto the same filename.
    private String targetKey(TopicTarget target) {
        return target.topicUrl() + "#" + target.level() + "#" +
                Objects.toString(target.mainCategory(), "") + "#" +
                String.join(">", target.topicNames().stream().map(n -> Objects.toString(n, "")).toList());
    }

    private void writeBatch(ExportData data) throws IOException {
        TopicExportContext ctx = data.topicContext();

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
                    List<ArticleLinkValue> topics = links != null ? links.topics() : List.of();
                    List<ArticleLinkValue> tags = links != null ? links.tags() : List.of();
                    return articleExportMapper.toDto(
                            article,
                            imagesByArticleId.getOrDefault(article.id(), List.of()),
                            videosByArticleId.getOrDefault(article.id(), List.of()),
                            topics,
                            tags);
                })
                .toList();

        for (ArticleExportDto dto : articles) {
            objectMapper.writeValue(generator, dto);
        }

        log.info("[EXPORT][topics] {} -> +{} articles", ctx.target().topicUrl(), articles.size());
    }

    /** Closes the currently open topic file properly. Used on a normal target switch and at
     *  shutdown — in both cases the generator is in a clean, structurally valid state. */
    private void closeCurrent() {
        if (generator == null) {
            return;
        }
        TopicTarget target = openTarget;
        try {
            generator.writeEndArray();   // articles
            generator.writeEndObject();  // topic-url wrapper
            generator.writeEndObject();  // root
            generator.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close export file for topic: " + target, e);
        } finally {
            generator = null;
            openTarget = null;
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
            log.warn("[EXPORT][topics] Failed to close broken generator for target {}", openTarget, e);
        } finally {
            generator = null;
            openTarget = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            closeCurrent();
        } catch (RuntimeException e) {
            log.warn("[EXPORT][topics] Failed to close export file for target {} during shutdown", openTarget, e);
        }
    }
}
