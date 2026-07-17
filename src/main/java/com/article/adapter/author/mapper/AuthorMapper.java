package com.article.adapter.author.mapper;


import com.article.adapter.author.persistance.AuthorEntity;
import com.article.domain.Author;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface AuthorMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"createdAt"})
    Author toAuthor(AuthorEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    AuthorEntity toEntity(Author author);
}
