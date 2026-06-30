package com.article.port;

import com.article.domain.ArticleImageRef;

import java.util.List;

public interface ImageDownloadPort {

    void download(ArticleImageRef imageRef);
}
