package com.article.adapter.article.mapper;

import com.article.adapter.article.persistence.entity.LinkArticleEntity;
import com.article.domain.ArticleLink;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface ArticleMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"id", "status", "errorMessage"})
    ArticleLink toArticleLink(LinkArticleEntity entity);

    List<ArticleLink> toArticleLinks(List<LinkArticleEntity> entities);
}
