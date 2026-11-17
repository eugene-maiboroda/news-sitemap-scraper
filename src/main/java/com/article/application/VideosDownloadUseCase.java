package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleVideoRef;
import com.article.domain.DownloadResult;
import com.article.domain.status.DownloadStatus;
import com.article.port.VideoDownloadPort;
import com.article.port.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideosDownloadUseCase {
    private final StepsProps stepsProps;
    private final VideoPersistencePort videoPersistencePort;
    private final VideoDownloadPort videoDownloadPort;
    private final ParallelExecutor parallelExecutor;

    public void downloadVideo() {
        log.info("Step: DOWNLOAD VIDEOS");

        int batchSize = stepsProps.downloadVideos().batchSize();
        List<ArticleVideoRef> batch = videoPersistencePort.findPending(batchSize);
        while (!batch.isEmpty()) {
            parallelExecutor.execute(batch, this::downloadVideoOne);
            batch = videoPersistencePort.findPending(batchSize);
        }
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
