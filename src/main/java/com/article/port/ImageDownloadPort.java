package com.article.port;

import com.article.domain.ArticleImageRef;
import com.article.domain.DownloadResult;

public interface ImageDownloadPort {

    DownloadResult download(ArticleImageRef imageRef);
}
