package com.article.port;

import com.article.domain.Author;
import com.article.domain.DownloadResult;

public interface AuthorAvatarDownloadPort {

    DownloadResult download(Author author);
}
