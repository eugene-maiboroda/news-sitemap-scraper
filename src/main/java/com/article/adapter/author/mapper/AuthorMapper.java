package com.article.adapter.author.mapper;


import com.article.adapter.author.persistence.entity.AuthorEntity;
import com.article.domain.author.Author;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface AuthorMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"createdAt", "updatedAt"})
    Author toAuthor(AuthorEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AuthorEntity toEntity(Author author);
}
