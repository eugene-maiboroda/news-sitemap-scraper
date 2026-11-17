package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleImageRef;
import com.article.domain.DownloadResult;
import com.article.domain.status.DownloadStatus;
import com.article.port.ImageDownloadPort;
import com.article.port.persistence.ImagePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImagesDownloaderUseCase {

    private final StepsProps stepsProps;
    private final ImagePersistencePort imagePersistencePort;
    private final ImageDownloadPort imageDownloadPort;
    private final ParallelExecutor parallelExecutor;

    public void downloadImages() {
        log.info("Step: DOWNLOAD IMAGES");

        int batchSize = stepsProps.downloadImages().batchSize();
        List<ArticleImageRef> batch = imagePersistencePort.findPending(batchSize);
        while (!batch.isEmpty()) {
            parallelExecutor.execute(batch, this::downloadOne);
            batch = imagePersistencePort.findPending(batchSize);
        }
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
