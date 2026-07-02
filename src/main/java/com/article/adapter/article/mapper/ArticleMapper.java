package com.article.adapter.article.mapper;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.adapter.article.persistence.entity.LinkArticleEntity;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(config = MapStructConfig.class)
public interface ArticleMapper {

    default String generateId() {
        return UUID.randomUUID().toString();
    }

    @BeanMapping(ignoreUnmappedSourceProperties = {"createdAt"})
    Article toArticle(ArticleEntity entity);

    List<Article> toArticles(List<ArticleEntity> entities);

    @Mapping(target = "createdAt", ignore = true)
    ArticleEntity toEntity(Article article);

    @BeanMapping(ignoreUnmappedSourceProperties = {"id", "status", "errorMessage"})
    ArticleLink toArticleLink(LinkArticleEntity entity);

    List<ArticleLink> toArticleLinks(List<LinkArticleEntity> entities);

    @Mapping(target = "id", expression = "java(generateId())")
    @Mapping(target = "status", expression = "java(com.article.domain.CrawlStatus.NEW)")
    @Mapping(target = "errorMessage", ignore = true)
    LinkArticleEntity toLinkEntity(ArticleLink link);
}
