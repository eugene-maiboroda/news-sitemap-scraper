package com.article.port.out;

import com.article.domain.author.Author;
import com.article.application.dto.DownloadResult;

public interface AuthorAvatarDownloadPort {

    DownloadResult download(Author author);
}
