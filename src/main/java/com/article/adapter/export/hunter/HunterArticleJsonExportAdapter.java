package com.article.adapter.export.hunter;

import com.article.adapter.export.JsonExportAdapter;
import com.article.bootstrap.properties.StepsProps;
import com.article.bootstrap.properties.StorageProps;
import com.article.port.ArticleJsonExportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "collector.site", havingValue = "AMERICAN_HUNTER", matchIfMissing = true)
public class HunterArticleJsonExportAdapter implements ArticleJsonExportPort {

    private static final String FILE_NAME = "american-hunter-articles.json";

    private final HunterArticleExportReader reader;
    private final JsonExportAdapter writer;
    private final StepsProps steps;
    private final StorageProps storage;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public void exportArticles() {
        int batchSize = steps.exportJson().batchSize();
        if (batchSize <= 0) {
            throw new IllegalArgumentException("steps.export-json.batch-size must be positive");
        }

        Path output = Path.of(storage.exportDir()).resolve(FILE_NAME);
        Iterable<HunterArticleExportDto> articles = () -> new ArticleIterator(batchSize);
        try {
            writer.export(output, "articles", articles);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write article export " + output, e);
        }
    }

    private final class ArticleIterator implements Iterator<HunterArticleExportDto> {

        private final int batchSize;
        private Iterator<HunterArticleExportDto> batch = List.<HunterArticleExportDto>of().iterator();
        private long offset;
        private boolean exhausted;

        private ArticleIterator(int batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public boolean hasNext() {
            if (!batch.hasNext() && !exhausted) {
                List<HunterArticleExportDto> articles = reader.readBatch(offset, batchSize);
                offset += articles.size();
                batch = articles.iterator();
                exhausted = articles.size() < batchSize;
            }
            return batch.hasNext();
        }

        @Override
        public HunterArticleExportDto next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return batch.next();
        }
    }
}
