package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.model.JsonExportTarget;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.adapter.export.resolver.ExportTargetResolver;
import com.article.application.dto.ArticleExportBatch;
import com.article.port.out.ArticleExportPort;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonExportAdapter implements ArticleExportPort {

    private final ArticleExportAssembler articleExportAssembler;
    private final ExportTargetResolver targetResolver;
    private final JsonExportFileWriter fileWriter;

    private boolean initialized;

    @Override
    public void export(ArticleExportBatch batch) {
        initialize();
        Map<JsonExportTarget, List<ArticleExportDto>> articlesByTarget = new LinkedHashMap<>();

        for (PreparedArticleExport article : articleExportAssembler.assemble(batch)) {
            for (JsonExportTarget target : targetResolver.resolve(article)) {
                articlesByTarget.computeIfAbsent(target, ignored -> new ArrayList<>())
                        .add(article.article());
            }
        }

        articlesByTarget.forEach(fileWriter::append);
    }

    @Override
    public void complete() {
        fileWriter.complete();
        initialized = false;
    }

    @PreDestroy
    public void shutdown() {
        complete();
    }

    private void initialize() {
        if (!initialized) {
            fileWriter.initialize(targetResolver.initialTargets());
            initialized = true;
        }
    }
}
