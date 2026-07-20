package com.article.adapter.video.persistence.entity;

import com.article.domain.media.DownloadStatus;
import com.article.domain.media.VideoPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class VideoEntity {

    @Id
    private String id;

    @Column(name = "article_id")
    private String articleId;

    @Enumerated(EnumType.STRING)
    private VideoPlatform platform;

    @Column(name = "jw_media_id")
    private String jwMediaId;

    @Column(name = "m3u8_url")
    private String m3u8Url;

    @Enumerated(EnumType.STRING)
    @Column(name = "download_status")
    @Builder.Default
    private DownloadStatus downloadStatus = DownloadStatus.PENDING;

    @Column(name = "local_file_path")
    private String localFilePath;

    @Column(name = "file_size")
    private Long fileSize;

    private String title;

    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "connatix_content_id")
    private String connatixContentId;

    @Column(name = "connatix_stream_id")
    private String connatixStreamId;

    @Column(name = "connatix_mmid")
    private String connatixMmid;

    @Column(name = "youtube_video_id")
    private String youtubeVideoId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void markDownloaded(String localFilePath, long fileSize) {
        this.downloadStatus = DownloadStatus.DOWNLOADED;
        this.localFilePath = localFilePath;
        this.fileSize = fileSize;
    }

    public void markFailed() {
        this.downloadStatus = DownloadStatus.FAILED;
    }
}
