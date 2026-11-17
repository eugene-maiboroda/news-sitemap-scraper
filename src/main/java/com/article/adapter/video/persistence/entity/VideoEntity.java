package com.article.adapter.video.persistence.entity;

import com.article.domain.status.DownloadStatus;
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

    @Column(name = "source_url")
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "download_status")
    @Builder.Default
    private DownloadStatus downloadStatus = DownloadStatus.PENDING;

    @Column(name = "local_file_path")
    private String localFilePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "youtube_video_id")
    private String youtubeVideoId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
