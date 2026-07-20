package com.article.adapter.author.persistence;

import com.article.adapter.author.mapper.AuthorMapper;
import com.article.adapter.author.persistence.entity.AuthorEntity;
import com.article.domain.author.Author;
import com.article.port.out.IdGenerationPort;
import com.article.port.out.persistence.AuthorPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorPersistenceService implements AuthorPersistencePort {

    private final AuthorRepository authorRepository;
    private final ArticleAuthorSourceRepository articleAuthorSourceRepository;
    private final AuthorMapper authorMapper;
    private final IdGenerationPort idGenerationPort;

    @Override
    public List<Author> findCandidates(int page, int pageSize) {
        Map<String, Author> authorsByUrl = new LinkedHashMap<>();
        articleAuthorSourceRepository
                .findDistinctByAuthorLinkIsNotNullAndAuthorIsNotNullOrderByAuthorLinkAsc(
                        PageRequest.of(page, pageSize)
                )
                .stream()
                .filter(candidate -> !candidate.author().isBlank() && !candidate.authorLink().isBlank())
                .forEach(candidate -> authorsByUrl.putIfAbsent(
                        candidate.authorLink(),
                        new Author(
                                idGenerationPort.generateId(),
                                candidate.author(),
                                candidate.authorLink(),
                                null,
                                null,
                                null
                        )
                ));
        return List.copyOf(authorsByUrl.values());
    }


    @Override
    @Transactional
    public void insert(List<Author> authors) {
        if (authors.isEmpty()) {
            return;
        }
        List<String> authorUrls = authors.stream()
                .map(Author::authorUrl)
                .distinct()
                .toList();
        Set<String> existingUrls = authorRepository.findByAuthorUrlIn(authorUrls).stream()
                .map(AuthorEntity::getAuthorUrl)
                .collect(Collectors.toSet());
        List<AuthorEntity> toSave = authors.stream()
                .filter(author -> existingUrls.add(author.authorUrl()))
                .map(authorMapper::toEntity)
                .toList();
        authorRepository.saveAll(toSave);
    }

    @Override
    public List<Author> findAll() {
        return authorRepository.findAll().stream()
                .map(authorMapper::toAuthor)
                .toList();
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
                .toList();
    }
}
