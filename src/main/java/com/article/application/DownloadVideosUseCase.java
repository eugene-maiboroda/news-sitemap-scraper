package com.article.application;

import com.article.application.dto.VideoDownloadData;
import com.article.application.dto.DownloadResult;
import com.article.domain.media.Video;
import com.article.domain.media.DownloadStatus;
import com.article.port.out.VideoDownloadPort;
import com.article.port.out.persistence.ArticlePersistencePort;
import com.article.port.out.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadVideosUseCase {
    private final ArticlePersistencePort articlePersistencePort;
    private final VideoPersistencePort videoPersistencePort;
    private final VideoDownloadPort videoDownloadPort;
    private final ParallelExecutor parallelExecutor;

    public void downloadVideos(int batchSize) {
        log.info("Step: DOWNLOAD VIDEOS");

        int batches = 0;
        int downloaded = 0;
        List<Video> batch;
        while (!(batch = videoPersistencePort.findPendingVideos(batchSize)).isEmpty()) {
            Map<String, String> categories = articlePersistencePort.findCategoriesByArticleIds(
                    batch.stream().map(Video::articleId).distinct().toList()
            );
            List<VideoDownloadData> downloadBatch = batch.stream()
                    .map(video -> toDownloadData(video, categories.get(video.articleId())))
                    .toList();

            parallelExecutor.execute(downloadBatch, this::downloadVideoOne);
            batches++;
            downloaded += batch.size();
            log.info("Downloaded video batch {} ({} videos)", batches, batch.size());
        }
        log.info("Download video finished ({} videos, {} batches)", downloaded, batches);
    }

    private void downloadVideoOne(VideoDownloadData data) {
        DownloadResult result = videoDownloadPort.download(data);
        if (result.status() == DownloadStatus.DOWNLOADED) {
            videoPersistencePort.markDownloaded(data.id(), result.localFilePath(), result.fileSize());
        } else {
            videoPersistencePort.markFailed(data.id());
        }
    }

    private VideoDownloadData toDownloadData(Video video, String category) {
        return new VideoDownloadData(
                video.id(),
                video.articleId(),
                category,
                video.platform(),
                video.m3u8Url(),
                video.youtubeVideoId()
        );
    }
}
