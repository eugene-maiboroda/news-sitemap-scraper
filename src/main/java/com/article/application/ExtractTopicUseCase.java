package com.article.application;


import com.article.adapter.article.MainCategoryResolver;
import com.article.adapter.article.TopicsTagsExtractor;
import com.article.adapter.export.dto.LinkDto;
import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleRawHtml;
import com.article.domain.ArticleTopicsResult;
import com.article.port.persistence.ArticleTopicsPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractTopicUseCase {
    private final StepsProps stepsProps;
    private final ArticleTopicsPersistencePort articleTopicsPersistencePort;
    private final ParallelExecutor parallelExecutor;
    private final TopicsTagsExtractor topicsTagsExtractor;
    private final MainCategoryResolver mainCategoryResolver;

    public void extractTopics() {
        log.info("Step: EXTRACT TOPICS");
        int batchSize = stepsProps.extractTopics().batchSize();
        int batchNumber = 0;
        int processed = 0;
        AtomicInteger topicOverflowCount = new AtomicInteger();
        List<ArticleRawHtml> batch;
        while (!(batch = articleTopicsPersistencePort.findPendingBatch(batchSize)).isEmpty()) {
            batchNumber++;
            List<ArticleTopicsResult> results =
                    parallelExecutor.map(batch, ref -> resolveTopics(ref, topicOverflowCount));
            if (!results.isEmpty()) {
                articleTopicsPersistencePort.saveTopicsBatch(results);
            }
            processed += batch.size();
            log.info("Processing batch {} | batch size: {} | processed so far: {}",
                    batchNumber, batch.size(), processed);
        }
        log.info("Extract topics finished ({} articles, {} batches)", processed, batchNumber);
        if (topicOverflowCount.get() > 0) {
            log.info("Articles with more than 5 topics: {}", topicOverflowCount.get());
        }
    }

    private ArticleTopicsResult resolveTopics(ArticleRawHtml ref, AtomicInteger topicOverflowCount) {
        List<LinkDto> topics = topicsTagsExtractor.extract(ref.articleId(), ref.rawHtml()).topics();
        if (topics.size() > 5) {
            topicOverflowCount.incrementAndGet();
            log.debug("Article {} has {} topics. Only first 5 topics will be stored.", ref.articleId(), topics.size());
        }
        String topic1Name = topics.isEmpty() ? null : topics.get(0).name();
        String topic1Url = topics.isEmpty() ? null : topics.get(0).url();
        String mainCategory = mainCategoryResolver.resolve(topic1Name, topic1Url);
        return new ArticleTopicsResult(ref.articleId(), topics, mainCategory);
    }
}
