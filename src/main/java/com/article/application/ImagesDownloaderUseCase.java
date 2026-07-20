package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleImageRef;
import com.article.domain.CategoryPendingCount;
import com.article.domain.DownloadResult;
import com.article.domain.status.DownloadStatus;
import com.article.port.ImageDownloadPort;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.persistence.ImagePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImagesDownloaderUseCase {

    private final StepsProps stepsProps;
    private final ArticlePersistencePort articlePersistencePort;
    private final ImagePersistencePort imagePersistencePort;
    private final ImageDownloadPort imageDownloadPort;
    private final ParallelExecutor parallelExecutor;

    public void downloadImages() {
        log.info("Step: DOWNLOAD IMAGES");

        List<String> allCategories = articlePersistencePort.findDistinctCategories();
        List<CategoryPendingCount> pendingCategories = imagePersistencePort.findCategoriesOrderedByPendingCount();
        log.info("Pending categories: {} of {} total", pendingCategories.size(), allCategories.size());

        int total = pendingCategories.size();
        int index = 0;
        for (CategoryPendingCount entry : pendingCategories) {
            index++;
            log.info("Category {}/{} — {} (pending: {})", index, total, entry.category(), entry.pendingCount());
            downloadBatches(entry.category(),
                    () -> imagePersistencePort.findPendingImagesForCategory(entry.category(), stepsProps.downloadImages().batchSize()));
        }

        log.info("Uncategorized — starting");
        downloadBatches("uncategorized",
                () -> imagePersistencePort.findPendingImagesWithoutCategory(stepsProps.downloadImages().batchSize()));

        log.info("Download finished");
    }

    private void downloadBatches(String label, Supplier<List<ArticleImageRef>> fetch) {
        int batches = 0;
        int downloaded = 0;
        List<ArticleImageRef> batch;
        while (!(batch = fetch.get()).isEmpty()) {
            parallelExecutor.execute(batch, this::downloadOne);
            batches++;
            downloaded += batch.size();
        }
        log.info("Category {} finished ({} images, {} batches)", label, downloaded, batches);
    }

    private void downloadOne(ArticleImageRef ref) {
        DownloadResult result = imageDownloadPort.download(ref);
        if (result.status() == DownloadStatus.DOWNLOADED) {
            imagePersistencePort.markDownloaded(ref.id(), result.localFilePath(), result.fileSize());
        } else {
            imagePersistencePort.markFailed(ref.id());
        }
    }
}
