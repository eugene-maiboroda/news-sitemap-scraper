package com.article.domain;

import com.article.domain.status.DownloadStatus;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    private String id;
    private String articleId;
    private String imageUrl;
    private String caption;
    private int position;
    private DownloadStatus downloadStatus;
    private String localFilePath;
    private Long fileSize;
}
