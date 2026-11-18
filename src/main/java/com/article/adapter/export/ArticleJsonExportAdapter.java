package com.article.adapter.export;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.StepsProps;
import com.article.bootstrap.properties.StorageProps;
import com.article.port.ArticleJsonExportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ArticleJsonExportAdapter implements ArticleJsonExportPort {

    private final ArticleExportReader reader;
    private final JsonExportAdapter writer;
    private final StepsProps steps;
    private final StorageProps storage;
    private final CollectorProps collector;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public void exportArticles() {
        int batchSize = steps.exportJson().batchSize();
        if (batchSize <= 0) {
            throw new IllegalArgumentException("steps.export-json.batch-size must be positive");
        }

        Path output = Path.of(storage.exportDir()).resolve(exportFileName());
        try {
            writer.export(output, "articles", entryWriter -> writeArticles(batchSize, entryWriter));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write article export " + output, e);
        }
    }

    private String exportFileName() {
        String configuredName = steps.exportJson().fileName();
        if (configuredName == null || configuredName.isBlank()) {
            return collector.site().name().toLowerCase(Locale.ROOT).replace('_', '-') + "-articles.json";
        }

        Path fileName = Path.of(configuredName);
        if (fileName.isAbsolute() || !configuredName.equals(fileName.getFileName().toString())
                || ".".equals(configuredName) || "..".equals(configuredName)) {
            throw new IllegalArgumentException("steps.export-json.file-name must be a file name");
        }
        return configuredName;
    }

    private void writeArticles(int batchSize, JsonExportAdapter.EntryWriter entryWriter) throws IOException {
        long offset = 0;
        while (true) {
            List<ArticleExportDto> batch = reader.readBatch(offset, batchSize);
            for (ArticleExportDto article : batch) {
                entryWriter.write(article);
            }

            if (batch.size() < batchSize) {
                return;
            }
            offset += batch.size();
        }
    }
}
