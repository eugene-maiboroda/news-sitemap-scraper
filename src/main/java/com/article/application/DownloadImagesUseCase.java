package com.article.application;

import com.article.application.dto.ImageDownloadData;
import com.article.domain.media.Image;
import com.article.application.dto.DownloadResult;
import com.article.domain.media.DownloadStatus;
import com.article.port.out.ImageDownloadPort;
import com.article.port.out.persistence.ArticlePersistencePort;
import com.article.port.out.persistence.ImagePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadImagesUseCase {

    private final ArticlePersistencePort articlePersistencePort;
    private final ImagePersistencePort imagePersistencePort;
    private final ImageDownloadPort imageDownloadPort;
    private final ParallelExecutor parallelExecutor;

    public void downloadImages(int batchSize) {
        log.info("Step: DOWNLOAD IMAGES");

        int batches = 0;
        int downloaded = 0;
        List<Image> batch;
        while (!(batch = imagePersistencePort.findPendingImages(batchSize)).isEmpty()) {
            Map<String, String> categories = articlePersistencePort.findCategoriesByArticleIds(
                    batch.stream().map(Image::articleId).distinct().toList()
            );
            List<ImageDownloadData> downloadBatch = batch.stream()
                    .map(image -> new ImageDownloadData(
                            image.id(),
                            image.articleId(),
                            categories.get(image.articleId()),
                            image.imageUrl(),
                            image.position()
                    ))
                    .toList();
            parallelExecutor.execute(downloadBatch, this::downloadOne);
            batches++;
            downloaded += downloadBatch.size();
        }
        log.info("Download finished ({} images, {} batches)", downloaded, batches);
    }

    private void downloadOne(ImageDownloadData data) {
        DownloadResult result = imageDownloadPort.download(data);
        if (result.status() == DownloadStatus.DOWNLOADED) {
            imagePersistencePort.markDownloaded(data.id(), result.localFilePath(), result.fileSize());
        } else {
            imagePersistencePort.markFailed(data.id());
        }
    }
}
