package com.article.port.out;

import com.article.application.dto.VideoDownloadData;
import com.article.application.dto.DownloadResult;

public interface VideoDownloadPort {

    DownloadResult download(VideoDownloadData data);
}
