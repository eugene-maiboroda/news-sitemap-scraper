package com.article.application;


import com.article.adapter.article.MainCategoryResolver;
import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleLinkValue;
import com.article.domain.ArticleLinks;
import com.article.domain.ArticleMetadataResult;
import com.article.domain.ArticleRawHtml;
import com.article.port.MetadataExtractorPort;
import com.article.port.persistence.ArticleTopicsPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractTopicUseCase {
    private final StepsProps stepsProps;
    private final ArticleTopicsPersistencePort articleTopicsPersistencePort;
    private final ParallelExecutor parallelExecutor;
    private final MetadataExtractorPort metadataExtractorPort;
    private final MainCategoryResolver mainCategoryResolver;

    public void extractTopics() {
        log.info("Step: EXTRACT TOPICS");
        int batchSize = stepsProps.extractMetadata().batchSize();
        int batchNumber = 0;
        int processed = 0;
        List<ArticleRawHtml> batch;
        while (!(batch = articleTopicsPersistencePort.findPendingBatch(batchSize)).isEmpty()) {
            batchNumber++;
            List<ArticleMetadataResult> results =
                    parallelExecutor.map(batch, this::resolveMetadata);
            if (!results.isEmpty()) {
                articleTopicsPersistencePort.saveArticleMetadata(results);
            }
            processed += batch.size();
            log.info("Processing batch {} | batch size: {} | processed so far: {}",
                    batchNumber, batch.size(), processed);
        }
        log.info("Extract topics finished ({} articles, {} batches)", processed, batchNumber);
    }

    private ArticleMetadataResult resolveMetadata(ArticleRawHtml ref) {
        ArticleLinks metadata = metadataExtractorPort.extract(ref.articleId(), ref.rawHtml());
        List<ArticleLinkValue> topics = metadata.topics();
        String topic1Name = topics.isEmpty() ? null : topics.get(0).name();
        String topic1Url = topics.isEmpty() ? null : topics.get(0).url();
        String mainCategory = mainCategoryResolver.resolve(topic1Name, topic1Url);
        return new ArticleMetadataResult(ref.articleId(), topics, metadata.tags(), mainCategory);
    }
}
