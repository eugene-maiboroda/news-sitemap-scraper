package com.article.domain;

import com.article.domain.status.DownloadStatus;
import com.article.domain.status.VideoPlatform;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    private String id;
    private String articleId;
    private VideoPlatform platform;
    private String jwMediaId;
    private String m3u8Url;
    private DownloadStatus downloadStatus;
    private String localFilePath;
    private Long fileSize;
    private String title;
    private String description;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Instant publishedAt;
    private String connatixContentId;
    private String connatixStreamId;
    private String connatixMmid;
    private String youtubeVideoId;
}
