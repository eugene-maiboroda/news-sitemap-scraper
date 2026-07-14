package com.article.domain;

import com.article.domain.status.DownloadStatus;

public record DownloadResult(DownloadStatus status, String localFilePath, Long fileSize) {

    public static DownloadResult downloaded(String localFilePath, long fileSize) {
        return new DownloadResult(DownloadStatus.DOWNLOADED, localFilePath, fileSize);
    }

    public static DownloadResult failed() {
        return new DownloadResult(DownloadStatus.FAILED, null, null);
    }
}