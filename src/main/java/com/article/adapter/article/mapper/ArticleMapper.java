package com.article.adapter.article.mapper;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.adapter.article.persistence.entity.ArticleLinkEntity;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface ArticleMapper {

    Article toArticle(ArticleEntity entity);

    List<Article> toArticles(List<ArticleEntity> entities);

    ArticleEntity toEntity(Article article);

    ArticleLink toArticleLink(ArticleLinkEntity entity);

    List<ArticleLink> toArticleLinks(List<ArticleLinkEntity> entities);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "parsed", expression = "java(false)")
    ArticleLinkEntity toLinkEntity(ArticleLink link);
}
