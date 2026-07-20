package com.article.adapter.author.persistence;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;

interface ArticleAuthorSourceRepository extends Repository<ArticleEntity, String> {

    List<AuthorCandidate> findDistinctByAuthorLinkIsNotNullAndAuthorIsNotNullOrderByAuthorLinkAsc(
            Pageable pageable
    );
}
