package com.article.port.persistence;

import com.article.domain.Author;

import java.util.List;

public interface AuthorPersistencePort {

    void insert(List<Author> authors);

    List<Author> findAll();

    void update(List<Author> authors);

    List<Author> findAuthorsPendingAvatarDownload();

}
