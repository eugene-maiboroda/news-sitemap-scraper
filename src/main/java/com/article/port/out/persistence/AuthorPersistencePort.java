package com.article.port.out.persistence;

import com.article.domain.author.Author;

import java.util.List;

public interface AuthorPersistencePort {

    List<Author> findCandidates(int page, int pageSize);

    void insert(List<Author> authors);

    List<Author> findAll();

    void update(List<Author> authors);

    List<Author> findAuthorsPendingAvatarDownload();

}
