package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.application.dto.ExportData;
import com.article.bootstrap.properties.StorageProps;
import com.article.domain.Article;
import com.article.domain.ArticleLinkValue;
import com.article.domain.ArticleLinks;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tags export mode: classifies articles into 4 fixed Sports Betting sections purely from tags
 * already stored in the article's JSONB metadata. Unlike
 * {@link TopicJsonExportAdapter} there is no "current target" and no file switching — the full
 * article table is scanned once in id order (see ExportJsonUseCase) and
 * matches for all 4 targets are interleaved across that single scan in arbitrary order, so all 4
 * files are opened together up front and closed together at the end.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "export.mode", havingValue = "tags")
public class SportsBettingJsonExportAdapter implements ExportPort {

    // Already normalized (trimmed, lowercase) — compared against normalizeTagNames(...) below, not
    // against unnormalized ArticleLinkValue.name() values. The extractor takes anchor.text() as-is
    // (Jsoup normalizes internal whitespace but never touches case), and the real NYPost tag-list
    // markup renders its visible ALL-CAPS look purely via CSS text-transform — the actual DOM text
    // (and so anchor.text()) is lowercase, e.g. "sports betting", "nfl betting". Comparing that
    // directly against title-case constants silently matched nothing.
    private static final String SPORTS_BETTING_TAG = "sports betting";

    private static final int MAX_DIAGNOSTIC_LOGS = 10;

    private final ObjectMapper objectMapper;
    private final StorageProps storageProps;
    private final ArticleExportMapper articleExportMapper;

    private final Map<SportsBettingExportTarget, JsonGenerator> generators = new EnumMap<>(SportsBettingExportTarget.class);
    private final Map<SportsBettingExportTarget, Integer> cumulativeTotals = new EnumMap<>(SportsBettingExportTarget.class);

    // Mirrors the page number in ExportJsonUseCase.exportJsonByTags() (export() is called
    // exactly once per page, in order, on a single thread) — kept locally instead of threading a
    // page number through ExportData, which is shared with category/topic modes.
    private int batchNumber = 0;

    // TEMPORARY diagnostics — logs raw vs. normalized tag names for the first few articles that
    // actually have tags, to make any future extraction/normalization mismatch visible immediately
    // instead of only showing up as a silent all-zero match count. Safe to remove once the tags
    // pipeline is confirmed stable in production.
    private int diagnosticsLogged = 0;

    @Override
    public void export(ExportData data) {
        // Opened lazily on the first real export() call, not at bean construction — mirrors
        // JsonExportAdapter/TopicJsonExportAdapter, which also only touch the export directory in
        // response to an actual export run. Configuring export.mode=tags must not create
        // files by itself when collector.steps.export-json=false (or any other step runs instead).
        if (generators.isEmpty()) {
            for (SportsBettingExportTarget target : SportsBettingExportTarget.values()) {
                generators.put(target, openNew(target));
                cumulativeTotals.put(target, 0);
            }
        }

        long start = System.currentTimeMillis();

        Map<String, List<Image>> imagesByArticleId = data.images().stream()
                .collect(Collectors.groupingBy(Image::getArticleId));

        Map<String, List<Video>> videosByArticleId = data.videos().stream()
                .collect(Collectors.groupingBy(Video::getArticleId));

        Map<String, ArticleLinks> linksByArticleId = data.articleLinks().stream()
                .collect(Collectors.toMap(ArticleLinks::articleId, Function.identity()));

        int sportsBettingCount = 0;
        Map<SportsBettingExportTarget, Integer> matches = new EnumMap<>(SportsBettingExportTarget.class);
        for (SportsBettingExportTarget target : SportsBettingExportTarget.values()) {
            matches.put(target, 0);
        }

        for (Article article : data.articles()) {
            ArticleLinks links = linksByArticleId.get(article.id());
            List<ArticleLinkValue> tags = links != null ? links.tags() : List.of();
            Set<String> tagNames = normalizeTagNames(tags);

            if (diagnosticsLogged < MAX_DIAGNOSTIC_LOGS && !tags.isEmpty()) {
                log.info("[EXPORT][tags][diagnostic] articleId={} rawTagNames={} normalizedTagNames={}",
                        article.id(), tags.stream().map(ArticleLinkValue::name).toList(), tagNames);
                diagnosticsLogged++;
            }

            if (!tagNames.contains(SPORTS_BETTING_TAG)) {
                continue;
            }
            sportsBettingCount++;

            List<ArticleLinkValue> topics = links != null ? links.topics() : List.of();
            ArticleExportDto dto = articleExportMapper.toDto(
                    article,
                    imagesByArticleId.getOrDefault(article.id(), List.of()),
                    videosByArticleId.getOrDefault(article.id(), List.of()),
                    topics,
                    tags);

            // An article may legitimately match several leagues at once (e.g. cross-posted content)
            // — it is written into every matching file, intentionally, no deduplication.
            for (SportsBettingExportTarget target : SportsBettingExportTarget.values()) {
                if (tagNames.contains(target.getRequiredTag().toLowerCase(Locale.ROOT))) {
                    writeArticle(target, dto);
                    matches.merge(target, 1, Integer::sum);
                }
            }
        }

        for (SportsBettingExportTarget target : SportsBettingExportTarget.values()) {
            cumulativeTotals.merge(target, matches.get(target), Integer::sum);
        }

        // Flush so file sizes on disk reflect progress during a long run, not only at shutdown —
        // ObjectMapper.writeValue() already flushes after each write, but a batch with zero matches
        // for a given league writes nothing at all, so that generator's preamble bytes (written with
        // low-level JsonGenerator calls in openNew(), which do NOT auto-flush) can otherwise sit
        // unflushed for a long time if that league's first real match appears much later in the scan.
        for (JsonGenerator generator : generators.values()) {
            try {
                generator.flush();
            } catch (IOException e) {
                log.warn("[EXPORT][tags] Failed to flush generator after batch {}", batchNumber, e);
            }
        }

        long writeMs = System.currentTimeMillis() - start;
        log.info("[EXPORT][tags] batch={} articles={} sportsBetting={} matches={} totals={} writeMs={}",
                batchNumber, data.articles().size(), sportsBettingCount, matches, cumulativeTotals, writeMs);
        batchNumber++;
    }

    // Normalizes case/whitespace before comparison rather than trusting the stored extractor output
    // output to already be in a comparable form (it isn't — see the SPORTS_BETTING_TAG comment).
    private Set<String> normalizeTagNames(List<ArticleLinkValue> tags) {
        return tags.stream()
                .map(ArticleLinkValue::name)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private void writeArticle(SportsBettingExportTarget target, ArticleExportDto dto) {
        try {
            objectMapper.writeValue(generators.get(target), dto);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export article for target: " + target, e);
        }
    }

    private JsonGenerator openNew(SportsBettingExportTarget target) {
        try {
            Path exportDir = Path.of(storageProps.exportDir());
            Files.createDirectories(exportDir);
            JsonGenerator generator = objectMapper.getFactory().createGenerator(
                    exportDir.resolve(target.getFileName()).toFile(), JsonEncoding.UTF8);
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();                             // root
            generator.writeObjectFieldStart(target.getTopicUrl());    // "<topicUrl>": {
            generator.writeArrayFieldStart("hierarchy");
            for (String level : target.getHierarchy()) {
                generator.writeString(level);
            }
            generator.writeEndArray();
            generator.writeArrayFieldStart("articles");
            log.info("[EXPORT][tags] opened {} for {}", target.getFileName(), target);
            return generator;
        } catch (IOException e) {
            throw new RuntimeException("Failed to open export file for target: " + target, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("[EXPORT][tags] shutdown | batchesProcessed={} totals={}", batchNumber, cumulativeTotals);
        for (Map.Entry<SportsBettingExportTarget, JsonGenerator> entry : generators.entrySet()) {
            try {
                JsonGenerator generator = entry.getValue();
                generator.writeEndArray();   // articles
                generator.writeEndObject();  // topic-url wrapper
                generator.writeEndObject();  // root
                generator.close();
            } catch (IOException e) {
                log.warn("[EXPORT][tags] Failed to close export file for target {}", entry.getKey(), e);
            }
        }
        generators.clear();
    }
}
