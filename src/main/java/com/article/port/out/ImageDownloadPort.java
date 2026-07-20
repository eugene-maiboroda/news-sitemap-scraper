package com.article.port.out;

import com.article.application.dto.ImageDownloadData;
import com.article.application.dto.DownloadResult;

public interface ImageDownloadPort {

    DownloadResult download(ImageDownloadData data);
}
