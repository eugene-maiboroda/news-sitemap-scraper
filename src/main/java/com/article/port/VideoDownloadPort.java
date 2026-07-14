package com.article.port;

import com.article.domain.ArticleVideoRef;
import com.article.domain.DownloadResult;

public interface VideoDownloadPort {

    DownloadResult download(ArticleVideoRef ref);
}
