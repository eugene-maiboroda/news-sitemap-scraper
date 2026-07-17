package com.article.adapter.author.persistance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorRepository extends JpaRepository<AuthorEntity, String> {

    // avatar_file_id IS NULL is itself the "pending download" signal — no separate status column
    // needed, since the file is skipped/re-derived deterministically from the author id either way.
    List<AuthorEntity> findByAvatarUrlIsNotNullAndAvatarFileIdIsNull();
}
