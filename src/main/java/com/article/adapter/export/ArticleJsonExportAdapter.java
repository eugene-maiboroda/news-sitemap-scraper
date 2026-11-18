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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

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
        Iterable<ArticleExportDto> articles = () -> new ArticleIterator(batchSize);
        try {
            writer.export(output, "articles", articles);
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

    private final class ArticleIterator implements Iterator<ArticleExportDto> {

        private final int batchSize;
        private Iterator<ArticleExportDto> batch = List.<ArticleExportDto>of().iterator();
        private long offset;
        private boolean exhausted;

        private ArticleIterator(int batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public boolean hasNext() {
            if (!batch.hasNext() && !exhausted) {
                List<ArticleExportDto> articles = reader.readBatch(offset, batchSize);
                offset += articles.size();
                batch = articles.iterator();
                exhausted = articles.size() < batchSize;
            }
            return batch.hasNext();
        }

        @Override
        public ArticleExportDto next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return batch.next();
        }
    }
}
