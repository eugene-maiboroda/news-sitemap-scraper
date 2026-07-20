package com.article.adapter.author.persistance;

import com.article.adapter.author.mapper.AuthorMapper;
import com.article.domain.Author;
import com.article.port.persistence.AuthorPersistencePort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorPersistenceService implements AuthorPersistencePort {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;


    @Override
    @Transactional
    public void insert(List<Author> authors) {

        List<AuthorEntity> toSave = authors.stream()
                .map(authorMapper::toEntity)
                .toList();

        authorRepository.saveAll(toSave);
    }

    @Override
    public List<Author> findAll() {
        List<AuthorEntity> authors = authorRepository.findAll();
        return authors.stream()
                .map(authorMapper::toAuthor)
                .collect(Collectors.toList());
    }

    @Override
    public void update(List<Author> authors) {
        List<AuthorEntity> entities = authors.stream()
                .map(authorMapper::toEntity)
                .toList();

        authorRepository.saveAll(entities);
    }

    @Override
    public List<Author> findAuthorsPendingAvatarDownload() {
        return authorRepository.findByAvatarUrlIsNotNullAndAvatarFileIdIsNull().stream()
                .map(authorMapper::toAuthor)
                .collect(Collectors.toList());
    }
}
