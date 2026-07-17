package com.article.port;

import com.article.domain.Author;

import java.util.List;

public interface AuthorExportPort {

    void export(List<Author> authors);
}
