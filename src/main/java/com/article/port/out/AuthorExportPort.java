package com.article.port.out;

import com.article.domain.author.Author;

import java.util.List;

public interface AuthorExportPort {

    void export(List<Author> authors);
}
