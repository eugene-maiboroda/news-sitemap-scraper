package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleVideoRef;
import com.article.domain.CategoryPendingCount;
import com.article.domain.DownloadResult;
import com.article.domain.status.DownloadStatus;
import com.article.port.VideoDownloadPort;
import com.article.port.persistence.ArticlePersistencePort;
import com.article.port.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideosDownloadUseCase {
    private final StepsProps stepsProps;
    private final ArticlePersistencePort articlePersistencePort;
    private final VideoPersistencePort videoPersistencePort;
    private final VideoDownloadPort videoDownloadPort;
    private final ParallelExecutor parallelExecutor;

    public void downloadVideo() {
        log.info("Step: DOWNLOAD VIDEOS");

        List<String> allCategories = articlePersistencePort.findDistinctCategories();
        List<CategoryPendingCount> pendingCategories = videoPersistencePort.findCategoriesOrderedByPendingCount();
        log.info("Pending categories: {} of {} total", pendingCategories.size(), allCategories.size());

        int total = pendingCategories.size();
        int index = 0;
        for (CategoryPendingCount entry : pendingCategories) {
            index++;
            log.info("Category {}/{} — {} (pending: {})", index, total, entry.category(), entry.pendingCount());
            downloadVideoBatches(entry.category(),
                    () -> videoPersistencePort.findPendingVideosForCategory(entry.category(), stepsProps.downloadVideos().batchSize()));
        }

        log.info("Uncategorized video — starting");
        downloadVideoBatches("uncategorized",
                () -> videoPersistencePort.findPendingVideosWithoutCategory(stepsProps.downloadVideos().batchSize()));

        log.info("Download video finished");
    }

    private void downloadVideoBatches(String label, Supplier<List<ArticleVideoRef>> fetch) {
        int batches = 0;
        int downloaded = 0;
        List<ArticleVideoRef> batch;
        while (!(batch = fetch.get()).isEmpty()) {
            parallelExecutor.execute(batch, this::downloadVideoOne);
            batches++;
            downloaded += batch.size();
        }
        log.info("Category {} finished ({} videos, {} batches)", label, downloaded, batches);
    }

    private void downloadVideoOne(ArticleVideoRef ref) {
        DownloadResult result = videoDownloadPort.download(ref);
        if (result.status() == DownloadStatus.DOWNLOADED) {
            videoPersistencePort.markDownloaded(ref.id(), result.localFilePath(), result.fileSize());
        } else {
            videoPersistencePort.markFailed(ref.id());
        }
    }

}
