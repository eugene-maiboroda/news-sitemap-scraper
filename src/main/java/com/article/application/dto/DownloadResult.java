package com.article.application.dto;

import com.article.domain.media.DownloadStatus;

public record DownloadResult(
        DownloadStatus status,
        String localFilePath,
        Long fileSize,
        String storedFileId
) {

    public static DownloadResult downloaded(String localFilePath, long fileSize) {
        return downloaded(localFilePath, fileSize, null);
    }

    public static DownloadResult downloaded(String localFilePath, long fileSize, String storedFileId) {
        return new DownloadResult(DownloadStatus.DOWNLOADED, localFilePath, fileSize, storedFileId);
    }

    public static DownloadResult failed() {
        return new DownloadResult(DownloadStatus.FAILED, null, null, null);
    }
}
